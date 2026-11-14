package com.r4bb1t.blockerspam.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.r4bb1t.blockerspam.MainActivity
import com.r4bb1t.blockerspam.R

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val UPDATE_CHANNEL_ID = "app_updates"
        private const val UPDATE_NOTIFICATION_ID = 2000
        private const val PREF_LAST_NOTIFIED_VERSION = "last_update_notified_version_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "App atualizado — notificando o usuário")
                notifyAppUpdated(context)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed — BlockerSpam monitoring active")
                // CallScreeningService is registered in the manifest and starts automatically.
            }
        }
    }

    /**
     * Mostra a notificação de "app atualizado" uma única vez por versão instalada.
     * O versionCode é gravado nas preferências, então reinstalar a mesma versão
     * ou reiniciar o aparelho não gera notificação repetida.
     */
    private fun notifyAppUpdated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Pacote não encontrado ao notificar atualização", e)
            return
        }

        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val versionName = packageInfo.versionName ?: "?"

        val prefs = context.getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
        if (prefs.getLong(PREF_LAST_NOTIFIED_VERSION, -1L) == versionCode) {
            return
        }
        prefs.edit().putLong(PREF_LAST_NOTIFIED_VERSION, versionCode).apply()

        ensureUpdateChannel(context)

        val pending = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone_blocked)
            .setContentTitle("BlockerSpam atualizado para v$versionName")
            .setContentText("Agora chamadas de números desconhecidos e privados também são bloqueadas.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "BlockerSpam foi atualizado para v$versionName. " +
                        "Agora chamadas de números desconhecidos e privados também são bloqueadas."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sem permissão para notificar atualização", e)
        }
    }

    private fun ensureUpdateChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Atualizações do app",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisa quando o BlockerSpam é atualizado"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
