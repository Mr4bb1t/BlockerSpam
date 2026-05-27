package com.r4bb1t.blockerspam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Fallback receiver for older Android versions (< API 29).
 * Full silent rejection is not possible without MODIFY_PHONE_STATE
 * (system-level). This receiver logs the call for history purposes.
 */
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return
        Log.d("CallReceiver", "Incoming call (legacy fallback). State: $state")
    }
}
