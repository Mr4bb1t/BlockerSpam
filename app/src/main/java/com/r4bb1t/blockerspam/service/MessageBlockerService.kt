package com.r4bb1t.blockerspam.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.r4bb1t.blockerspam.data.BlockedMessage
import com.r4bb1t.blockerspam.data.CallDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageBlockerService : NotificationListenerService() {

    private val db by lazy { CallDatabase.getInstance(this) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val prefs = getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("pref_message_blocking_enabled", true)
        if (!enabled) return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""

        val content = "$title $text $bigText".lowercase()
        
        // Let's filter out non-messaging apps if needed, but for now any notification with words
        // We can restrict it to known messaging apps or just any app.
        // SMS packages typically have "sms", "mms", "messaging", "whatsapp", "telegram".
        val packageName = sbn.packageName.lowercase()
        val isMessagingApp = packageName.contains("sms") || packageName.contains("mms") || 
                             packageName.contains("messaging") || packageName.contains("whatsapp") || 
                             packageName.contains("telegram") || packageName.contains("messenger")

        // We only check if it is a messaging app or we check everything?
        // Checking everything could be aggressive, but let's check everything or let the user decide.
        // For safety, checking everything is what the user probably implies with "quando a mensagem vem... qualquer lugar da mensagem".

        val defaultWords = setOf("vivo", "tim", "claro", "oi", "bet")
        val keywords = prefs.getStringSet("pref_message_keywords", defaultWords) ?: defaultWords

        if (keywords.isEmpty()) return

        for (word in keywords) {
            if (word.isNotBlank() && content.contains(word.lowercase())) {
                Log.d("MessageBlocker", "Blocked notification containing: $word")
                cancelNotification(sbn.key)

                // Save to database
                CoroutineScope(Dispatchers.IO).launch {
                    val sender = title.ifBlank { "Desconhecido" }
                    val messageContent = if (text.isNotBlank()) text else bigText
                    db.callDao().insertBlockedMessage(
                        BlockedMessage(
                            sender = sender,
                            content = messageContent,
                            matchedKeyword = word
                        )
                    )
                }
                break
            }
        }
    }
}
