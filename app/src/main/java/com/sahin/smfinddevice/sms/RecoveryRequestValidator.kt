package com.sahin.smfinddevice.sms

import com.sahin.smfinddevice.data.RecoveryCommand
import com.sahin.smfinddevice.data.RecoveryNumber
import com.sahin.smfinddevice.security.PhoneNumberNormalizer
import com.sahin.smfinddevice.storage.SecureConfigStore

sealed class ValidationResult {
    data class Authorized(val number: RecoveryNumber, val command: RecoveryCommand) : ValidationResult()
    object UnauthorizedSender : ValidationResult()
    object UnrecognizedCommand : ValidationResult()
    object Duplicate : ValidationResult()
    object CooldownActive : ValidationResult()
}

/**
 * Implements the mandatory two-factor check from the spec:
 *   1. Sender must be an enabled + verified recovery number.
 *   2. Message must exactly match an enabled command after normalization.
 * Both must pass before any location work starts.
 */
class RecoveryRequestValidator(
    private val store: SecureConfigStore,
    private val normalizer: PhoneNumberNormalizer,
    private val cooldownMillis: Long = 30_000L
) {

    fun validate(senderAddress: String, body: String, requestId: String): ValidationResult {
        if (store.hasSeenRequestId(requestId)) {
            return ValidationResult.Duplicate
        }

        // Check 1: authorized sender (must be one of possibly many enabled+verified numbers).
        val authorizedNumber = store.findAuthorizedNumberForSender(senderAddress, normalizer)
            ?: return ValidationResult.UnauthorizedSender

        // Check 2: exact normalized command match against enabled commands only.
        val normalizedBody = RecoveryCommand.normalize(body)
        val matchedCommand = store.enabledCommands().firstOrNull { it.normalized == normalizedBody }
            ?: return ValidationResult.UnrecognizedCommand

        val now = System.currentTimeMillis()
        if (now - store.lastRecoveryRequestAt < cooldownMillis) {
            return ValidationResult.CooldownActive
        }

        store.rememberRequestId(requestId)
        return ValidationResult.Authorized(authorizedNumber, matchedCommand)
    }
}
