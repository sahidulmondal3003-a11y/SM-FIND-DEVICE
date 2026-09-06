package com.sahin.smfinddevice.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sahin.smfinddevice.security.PhoneNumberNormalizer
import com.sahin.smfinddevice.service.LocationRecoveryService
import com.sahin.smfinddevice.storage.SecureConfigStore
import com.sahin.smfinddevice.utils.SafeLog
import java.security.MessageDigest

class RecoverySmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val store = SecureConfigStore.getInstance(context)
        if (!store.protectionEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Multi-part SMS: concatenate all parts from the same sender into one body.
        val sender = messages.first().originatingAddress ?: return
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        // Loop prevention (section 38): our own outgoing responses always start with this marker.
        // A message carrying it can never be treated as an inbound recovery command.
        if (fullBody.trimStart().startsWith(RecoverySmsSender.RESPONSE_MARKER)) {
            SafeLog.d("Ignored inbound message matching our own outgoing response marker.")
            return
        }

        val requestId = buildRequestId(sender, fullBody)

        val normalizer = PhoneNumberNormalizer()
        val validator = RecoveryRequestValidator(store, normalizer)

        when (val result = validator.validate(sender, fullBody, requestId)) {
            is ValidationResult.Authorized -> {
                store.markUsedNow(result.number.id)
                store.lastRecoveryRequestAt = System.currentTimeMillis()
                LocationRecoveryService.enqueueRecoveryRequest(
                    context = context,
                    recipientE164 = result.number.e164Number
                )
            }
            ValidationResult.UnauthorizedSender,
            ValidationResult.UnrecognizedCommand,
            ValidationResult.Duplicate,
            ValidationResult.CooldownActive -> {
                // Section 6 / 36: never reveal location, never fake success, just stay silent
                // to unauthorized or unrecognized traffic. No further action.
                SafeLog.d("Recovery SMS request not processed: ${result::class.simpleName}")
            }
        }
    }

    /** Stable per-message identifier for duplicate-delivery protection (section 39). */
    private fun buildRequestId(sender: String, body: String): String {
        // Bucket by minute so genuinely repeated *new* commands a minute apart are treated
        // as separate requests, while redelivery of the exact same SMS is deduplicated.
        val minuteBucket = System.currentTimeMillis() / 60_000
        val raw = "$sender|$body|$minuteBucket"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
