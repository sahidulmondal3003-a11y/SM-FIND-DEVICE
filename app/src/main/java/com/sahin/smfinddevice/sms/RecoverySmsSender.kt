package com.sahin.smfinddevice.sms

import android.content.Context
import android.telephony.SmsManager
import com.sahin.smfinddevice.data.LocationFailureReason
import com.sahin.smfinddevice.data.LocationOutcome
import com.sahin.smfinddevice.utils.SafeLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sends the recovery SMS reply to the specific authorized number that made the request
 * (never broadcast to all saved numbers). Every outgoing message is tagged with
 * [RESPONSE_MARKER] so RecoverySmsReceiver can immediately discard it if it were ever
 * somehow re-delivered as an inbound message (self-trigger / loop prevention, section 38).
 */
class RecoverySmsSender(private val context: Context) {

    companion object {
        /** Never appears in any of the 15 command texts; safe as a loop-prevention marker. */
        const val RESPONSE_MARKER = "SM Find Device"
        private const val DIVIDER = "━━━━━━━━━━━━━━━━━━"
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun sendOutcome(toE164Number: String, outcome: LocationOutcome) {
        val body = when (outcome) {
            is LocationOutcome.Success -> buildSuccessMessage(outcome.result)
            is LocationOutcome.FallbackToLastKnown -> buildFallbackMessage(outcome.result)
            is LocationOutcome.Failure -> buildFailureMessage(outcome.reason)
        }
        send(toE164Number, body)
    }

    fun sendPlainNotice(toE164Number: String, notice: String) {
        send(toE164Number, "$RESPONSE_MARKER\n$notice\n$DIVIDER\nDesign & Developed By SAHIN")
    }

    private fun buildSuccessMessage(result: com.sahin.smfinddevice.data.LocationResult): String {
        return """
            $RESPONSE_MARKER
            LOCATION FOUND
            $DIVIDER
            Maps:
            ${result.mapsUrl()}
            Accuracy: Approximately ${result.accuracyMeters.toInt()} meters
            Updated: ${dateFormat.format(Date(result.timestampMillis))}
            $DIVIDER
            Design & Developed By SAHIN
        """.trimIndent()
    }

    private fun buildFallbackMessage(result: com.sahin.smfinddevice.data.LocationResult): String {
        return """
            $RESPONSE_MARKER
            LAST KNOWN LOCATION
            $DIVIDER
            Maps:
            ${result.mapsUrl()}
            Accuracy: Approximately ${result.accuracyMeters.toInt()} meters
            Last updated: ${dateFormat.format(Date(result.timestampMillis))}
            $DIVIDER
            Design & Developed By SAHIN
        """.trimIndent()
    }

    private fun buildFailureMessage(reason: LocationFailureReason): String {
        val explanation = when (reason) {
            LocationFailureReason.LOCATION_PERMISSION_DENIED -> "Location permission is not granted on this device."
            LocationFailureReason.PRECISE_LOCATION_DENIED -> "Only approximate location is permitted on this device."
            LocationFailureReason.GPS_DISABLED -> "GPS is turned off on this device."
            LocationFailureReason.LOCATION_SERVICES_DISABLED -> "Location services are turned off on this device."
            LocationFailureReason.SIGNAL_UNAVAILABLE -> "No location signal is currently available."
            LocationFailureReason.TIMEOUT -> "A fresh location could not be acquired in time."
            LocationFailureReason.NO_LAST_KNOWN_LOCATION -> "No location, fresh or last-known, is available."
            LocationFailureReason.AIRPLANE_MODE -> "The device appears to be in airplane mode."
            LocationFailureReason.UNKNOWN -> "Location could not be determined right now."
        }
        return """
            $RESPONSE_MARKER
            LOCATION RECOVERY FAILED
            $DIVIDER
            $explanation
            $DIVIDER
            Design & Developed By SAHIN
        """.trimIndent()
    }

    private fun send(toE164Number: String, body: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
                ?: SmsManager.getDefault()
            val parts = smsManager.divideMessage(body)
            smsManager.sendMultipartTextMessage(toE164Number, null, parts, null, null)
            // Never log the number or body itself — section 30.
            SafeLog.d("Recovery SMS response sent.")
        } catch (e: Exception) {
            SafeLog.e("Failed to send recovery SMS response", e)
        }
    }
}
