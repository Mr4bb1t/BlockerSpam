package com.r4bb1t.blockerspam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Boot completed — BlockerSpam monitoring active")
        // CallScreeningService is registered in the manifest and starts automatically.
        // No explicit restart needed; just a hook for future foreground service.
    }
}
