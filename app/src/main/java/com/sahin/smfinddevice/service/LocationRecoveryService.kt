package com.sahin.smfinddevice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sahin.smfinddevice.data.ActivityCategory
import com.sahin.smfinddevice.data.ActivityEvent
import com.sahin.smfinddevice.data.ActivityStatus
import com.sahin.smfinddevice.data.LocationOutcome
import com.sahin.smfinddevice.location.LocationAcquisitionEngine
import com.sahin.smfinddevice.sms.RecoverySmsSender
import com.sahin.smfinddevice.storage.SecureConfigStore
import com.sahin.smfinddevice.ui.MainActivity
import com.sahin.smfinddevice.utils.SafeLog
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs only for the duration of a single location-recovery request (section 14/29: no
 * permanent background service). Started by [RecoverySmsReceiver] once a request passes
 * both authorization checks, or by the UI for "Test Recovery".
 */
class LocationRecoveryService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val recipient = intent?.getStringExtra(EXTRA_RECIPIENT_E164)
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false

        startForeground(NOTIFICATION_ID, buildInProgressNotification())

        if (recipient == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            val engine = LocationAcquisitionEngine(applicationContext)
            val outcome = engine.acquireLocation()
            handleOutcome(recipient, outcome, isTest)
            stopForegroundCompat()
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun handleOutcome(recipient: String, outcome: LocationOutcome, isTest: Boolean) {
        val store = SecureConfigStore.getInstance(applicationContext)
        val category = if (isTest) ActivityCategory.RECOVERY_TEST else ActivityCategory.SMS_RECOVERY

        when (outcome) {
            is LocationOutcome.Success -> {
                store.rememberLastLocationResult(outcome.result)
                if (isTest) store.lastSuccessfulTestAt = System.currentTimeMillis()
                store.appendActivityEvent(
                    ActivityEvent(
                        id = UUID.randomUUID().toString(),
                        category = category,
                        title = if (isTest) "Recovery Test" else "SMS Recovery",
                        detail = "Location acquired",
                        timestampMillis = System.currentTimeMillis(),
                        status = ActivityStatus.SUCCESS,
                        accuracyMeters = outcome.result.accuracyMeters
                    )
                )
            }
            is LocationOutcome.FallbackToLastKnown -> {
                store.rememberLastLocationResult(outcome.result)
                store.appendActivityEvent(
                    ActivityEvent(
                        id = UUID.randomUUID().toString(),
                        category = category,
                        title = if (isTest) "Recovery Test" else "SMS Recovery",
                        detail = "Last known location used (fresh fix unavailable)",
                        timestampMillis = System.currentTimeMillis(),
                        status = ActivityStatus.WARNING,
                        accuracyMeters = outcome.result.accuracyMeters
                    )
                )
            }
            is LocationOutcome.Failure -> {
                SafeLog.d("Location recovery failed: ${outcome.reason}")
                store.appendActivityEvent(
                    ActivityEvent(
                        id = UUID.randomUUID().toString(),
                        category = category,
                        title = if (isTest) "Recovery Test" else "SMS Recovery",
                        detail = "Location unavailable: ${outcome.reason}",
                        timestampMillis = System.currentTimeMillis(),
                        status = ActivityStatus.FAILURE
                    )
                )
            }
        }

        if (!isTest) {
            RecoverySmsSender(applicationContext).sendOutcome(recipient, outcome)
        } else {
            // Test Recovery is owner-visible in-app only; per spec it must be clearly
            // labeled as a test and should not silently send a real SMS unless the
            // owner explicitly chose "send test SMS to this number" in the UI.
            SafeLog.d("Test recovery completed; result surfaced in-app only.")
        }

        notifyCompletion(outcome)
    }

    private fun buildInProgressNotification(): Notification {
        ensureChannel()
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SM Find Device")
            .setContentText("Location recovery operation in progress")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun notifyCompletion(outcome: LocationOutcome) {
        ensureChannel()
        val text = when (outcome) {
            is LocationOutcome.Success -> "Location recovered and sent to the requesting number."
            is LocationOutcome.FallbackToLastKnown -> "Fresh location unavailable; last-known location sent."
            is LocationOutcome.Failure -> "Location recovery could not be completed."
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SM Find Device")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                manager?.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Recovery Operations",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "recovery_operations"
        private const val NOTIFICATION_ID = 4200
        private const val EXTRA_RECIPIENT_E164 = "extra_recipient_e164"
        private const val EXTRA_IS_TEST = "extra_is_test"

        fun enqueueRecoveryRequest(context: Context, recipientE164: String, isTest: Boolean = false) {
            val intent = Intent(context, LocationRecoveryService::class.java)
                .putExtra(EXTRA_RECIPIENT_E164, recipientE164)
                .putExtra(EXTRA_IS_TEST, isTest)
            context.startForegroundService(intent)
        }
    }
}
