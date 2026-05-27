package com.r4bb1t.blockerspam.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.r4bb1t.blockerspam.MainActivity
import com.r4bb1t.blockerspam.R
import com.r4bb1t.blockerspam.data.BlockedCall
import com.r4bb1t.blockerspam.data.CallDatabase
import com.r4bb1t.blockerspam.helper.ContactsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
class BlockerCallScreeningService : CallScreeningService() {

    companion object {
        const val CHANNEL_ID = "blocker_channel"
        const val PREF_BLOCKING_ENABLED = "blocking_enabled"
        private var notifId = 1000
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: run {
            allowCall(callDetails)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
            val blockingEnabled = prefs.getBoolean(PREF_BLOCKING_ENABLED, true)

            val db = CallDatabase.getInstance(applicationContext)
            val isWhitelisted = db.callDao().isWhitelisted(number)
            val isInContacts = ContactsHelper.isNumberInContacts(applicationContext, number)

            val shouldBlock = blockingEnabled && !isWhitelisted && !isInContacts

            if (shouldBlock) {
                db.callDao().insertBlockedCall(BlockedCall(number = number))
                sendBlockNotification(number)
                rejectCall(callDetails)
            } else {
                allowCall(callDetails)
            }
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
            .setContentText("Número desconhecido: $number")
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
