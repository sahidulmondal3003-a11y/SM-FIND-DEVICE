package com.sahin.smfinddevice.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sahin.smfinddevice.storage.SecureConfigStore
import com.sahin.smfinddevice.utils.SafeLog

/**
 * After reboot, Android's own SMS_RECEIVED broadcast + our manifest-registered
 * RecoverySmsReceiver already resume working with no action needed here — this
 * receiver exists only to log (debug builds only) that boot state was observed,
 * per section 31 Test 12. It never silently re-enables protection the owner disabled.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = SecureConfigStore.getInstance(context)
        SafeLog.d("Device rebooted. Protection enabled=${store.protectionEnabled}")
    }
}
