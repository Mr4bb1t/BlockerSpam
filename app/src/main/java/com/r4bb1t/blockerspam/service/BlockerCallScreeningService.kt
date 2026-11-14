package com.r4bb1t.blockerspam.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.r4bb1t.blockerspam.MainActivity
import com.r4bb1t.blockerspam.R
import com.r4bb1t.blockerspam.data.BlockedCall
import com.r4bb1t.blockerspam.data.CallDatabase
import com.r4bb1t.blockerspam.helper.ContactsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@RequiresApi(Build.VERSION_CODES.Q)
class BlockerCallScreeningService : CallScreeningService() {

    companion object {
        const val CHANNEL_ID = "blocker_channel"
        const val PREF_BLOCKING_ENABLED = "blocking_enabled"
        const val UNKNOWN_NUMBER = "Número desconhecido"
        const val PRIVATE_NUMBER = "Número privado"

        private const val TAG = "BlockerScreening"
        private const val DECISION_TIMEOUT_MS = 3_500L
        private var notifId = 1000
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val prefs = getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
        val blockingEnabled = prefs.getBoolean(PREF_BLOCKING_ENABLED, true)
        if (!blockingEnabled) {
            allowCall(callDetails)
            return
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart?.trim().orEmpty()
        val cleanNumber = rawNumber.replace(Regex("[^+\\d]"), "")

        // Chamadas sem número (privadas/restritas) precisam ser bloqueadas aqui.
        // Antes o app simplesmente liberava a chamada quando o handle vinha nulo.
        if (cleanNumber.isBlank() || hasHiddenCallerId(callDetails)) {
            val label = if (cleanNumber.isBlank()) UNKNOWN_NUMBER else PRIVATE_NUMBER
            blockCall(callDetails, label)
            return
        }

        // Números conhecidos: verifica whitelist e contatos sem travar a thread
        // principal, mas sempre responde à chamada mesmo se algo falhar.
        serviceScope.launch {
            val shouldBlock = try {
                withTimeoutOrNull(DECISION_TIMEOUT_MS) {
                    val db = CallDatabase.getInstance(applicationContext)
                    val isWhitelisted = db.callDao().isWhitelisted(cleanNumber) ||
                        db.callDao().isWhitelisted(rawNumber)
                    val isInContacts = ContactsHelper.isNumberInContacts(applicationContext, cleanNumber)
                    !isWhitelisted && !isInContacts
                } ?: false
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao decidir sobre a chamada $rawNumber; liberando", t)
                false
            }

            if (shouldBlock) {
                blockCall(callDetails, rawNumber)
            } else {
                allowCall(callDetails)
            }
        }
    }

    private fun hasHiddenCallerId(callDetails: Call.Details): Boolean {
        return when (callDetails.handlePresentation) {
            TelecomManager.PRESENTATION_RESTRICTED,
            TelecomManager.PRESENTATION_UNAVAILABLE,
            TelecomManager.PRESENTATION_PAYPHONE -> true
            else -> false
        }
    }

    /**
     * Responde a chamada imediatamente e só depois grava no histórico/notifica.
     * Assim uma falha no banco nunca deixa a ligação tocar.
     */
    private fun blockCall(callDetails: Call.Details, number: String) {
        rejectCall(callDetails)
        serviceScope.launch {
            try {
                val db = CallDatabase.getInstance(applicationContext)
                db.callDao().insertBlockedCall(BlockedCall(number = number))
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao gravar chamada bloqueada", t)
            }
            sendBlockNotification(number)
        }
    }

    private fun rejectCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setRejectCall(true)
            .setDisallowCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)
    }

    private fun allowCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setRejectCall(false)
            .setDisallowCall(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun sendBlockNotification(number: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureNotificationChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone_blocked)
            .setContentTitle("Ligação bloqueada")
            .setContentText(number)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId++, notif)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ligações Bloqueadas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notificações de chamadas bloqueadas automaticamente" }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
