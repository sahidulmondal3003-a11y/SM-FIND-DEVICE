package com.sahin.smfinddevice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sahin.smfinddevice.data.ActivityCategory
import com.sahin.smfinddevice.data.ActivityEvent
import com.sahin.smfinddevice.data.ActivityStatus
import com.sahin.smfinddevice.data.LocationOutcome
import com.sahin.smfinddevice.data.LocationResult
import com.sahin.smfinddevice.data.ProtectionState
import com.sahin.smfinddevice.data.ProtectionStateEvaluator
import com.sahin.smfinddevice.location.LocationAcquisitionEngine
import com.sahin.smfinddevice.permissions.PermissionChecker
import com.sahin.smfinddevice.storage.SecureConfigStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val protectionState: ProtectionState = ProtectionState.PROTECTION_DISABLED,
    val activeRecoveryNumberCount: Int = 0,
    val enabledCommandCount: Int = 0,
    val lastTestOutcome: LocationOutcome? = null,
    val isRunningTest: Boolean = false,
    val lastKnownLocation: LocationResult? = null,
    val lastRecoveryRequestAt: Long = 0L,
    val lastSuccessfulTestAt: Long = 0L,
    val checklist: ProtectionStateEvaluator.Checklist? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SecureConfigStore.getInstance(application)
    private val permissionChecker = PermissionChecker(application)
    private val evaluator = ProtectionStateEvaluator(permissionChecker, store)
    private val locationEngine = LocationAcquisitionEngine(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            store.recoveryNumbers.collect { refresh() }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(
            protectionState = evaluator.currentState(),
            activeRecoveryNumberCount = store.recoveryNumbers.value.count { it.enabled && it.verified },
            enabledCommandCount = store.enabledCommands().size,
            lastKnownLocation = store.lastLocationResult(),
            lastRecoveryRequestAt = store.lastRecoveryRequestAt,
            lastSuccessfulTestAt = store.lastSuccessfulTestAt,
            checklist = evaluator.evaluateChecklist()
        )
    }

    fun setProtectionEnabled(enabled: Boolean) {
        store.protectionEnabled = enabled
        store.appendActivityEvent(
            ActivityEvent(
                id = UUID.randomUUID().toString(),
                category = ActivityCategory.PROTECTION,
                title = if (enabled) "Protection Enabled" else "Protection Disabled",
                detail = if (enabled) "Device is ready to respond to authorized recovery SMS" else "Recovery SMS will be ignored",
                timestampMillis = System.currentTimeMillis(),
                status = if (enabled) ActivityStatus.SUCCESS else ActivityStatus.WARNING
            )
        )
        refresh()
    }

    /** Backs both the Home "Test Location" quick action and the Recovery In Action screen. */
    fun runTestLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningTest = true)
            val outcome = locationEngine.acquireLocationForTest()
            when (outcome) {
                is LocationOutcome.Success -> {
                    store.rememberLastLocationResult(outcome.result)
                    store.lastSuccessfulTestAt = System.currentTimeMillis()
                    logTestEvent("Location acquired", ActivityStatus.SUCCESS, outcome.result.accuracyMeters)
                }
                is LocationOutcome.FallbackToLastKnown -> {
                    store.rememberLastLocationResult(outcome.result)
                    logTestEvent("Last known location used (fresh fix unavailable)", ActivityStatus.WARNING, outcome.result.accuracyMeters)
                }
                is LocationOutcome.Failure -> {
                    logTestEvent("Location unavailable: ${outcome.reason}", ActivityStatus.FAILURE, null)
                }
            }
            _uiState.value = _uiState.value.copy(isRunningTest = false, lastTestOutcome = outcome)
            refresh()
        }
    }

    private fun logTestEvent(detail: String, status: ActivityStatus, accuracy: Float?) {
        store.appendActivityEvent(
            ActivityEvent(
                id = UUID.randomUUID().toString(),
                category = ActivityCategory.RECOVERY_TEST,
                title = "Test Location",
                detail = detail,
                timestampMillis = System.currentTimeMillis(),
                status = status,
                accuracyMeters = accuracy
            )
        )
    }

    /** Clears the transient in-memory result so re-opening Recovery In Action starts fresh. */
    fun clearTestOutcome() {
        _uiState.value = _uiState.value.copy(lastTestOutcome = null)
    }
}
