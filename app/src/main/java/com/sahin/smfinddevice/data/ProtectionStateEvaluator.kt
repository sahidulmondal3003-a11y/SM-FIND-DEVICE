package com.sahin.smfinddevice.data

import com.sahin.smfinddevice.permissions.PermissionChecker
import com.sahin.smfinddevice.storage.SecureConfigStore

/**
 * Section 34: "PROTECTION ACTIVE" may only be shown once every item in the
 * checklist is genuinely true — never optimistically.
 */
class ProtectionStateEvaluator(
    private val permissionChecker: PermissionChecker,
    private val store: SecureConfigStore
) {
    data class Checklist(
        val locationPermission: Boolean,
        val preciseLocation: Boolean,
        val backgroundCapability: Boolean,
        val smsCapability: Boolean,
        val hasRecoveryNumber: Boolean,
        val hasEnabledCommand: Boolean,
        val ownerEnabledProtection: Boolean
    ) {
        val allRequiredMet: Boolean
            get() = locationPermission && smsCapability && hasRecoveryNumber &&
                hasEnabledCommand && ownerEnabledProtection
    }

    fun evaluateChecklist(): Checklist = Checklist(
        locationPermission = permissionChecker.hasAnyLocationPermission(),
        preciseLocation = permissionChecker.hasPreciseLocationPermission(),
        backgroundCapability = permissionChecker.hasBackgroundLocationPermission(),
        smsCapability = permissionChecker.hasSendSmsPermission() && permissionChecker.hasReceiveSmsPermission(),
        hasRecoveryNumber = store.hasAnyActiveRecoveryNumber(),
        hasEnabledCommand = store.enabledCommands().isNotEmpty(),
        ownerEnabledProtection = store.protectionEnabled
    )

    fun currentState(): ProtectionState {
        if (!store.protectionEnabled) return ProtectionState.PROTECTION_DISABLED
        val checklist = evaluateChecklist()
        return when {
            !checklist.allRequiredMet -> ProtectionState.ACTION_REQUIRED
            !checklist.preciseLocation || !checklist.backgroundCapability -> ProtectionState.LIMITED_PROTECTION
            else -> ProtectionState.PROTECTION_ACTIVE
        }
    }
}
