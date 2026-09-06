package com.sahin.smfinddevice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sahin.smfinddevice.data.ActivityCategory
import com.sahin.smfinddevice.data.ActivityEvent
import com.sahin.smfinddevice.data.ActivityStatus
import com.sahin.smfinddevice.data.RecoveryNumber
import com.sahin.smfinddevice.security.PhoneNumberNormalizer
import com.sahin.smfinddevice.service.LocationRecoveryService
import com.sahin.smfinddevice.sms.RecoverySmsSender
import com.sahin.smfinddevice.storage.SecureConfigStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class RecoveryNumbersUiState(
    val numbers: List<RecoveryNumber> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val hasNoActiveNumber: Boolean get() = numbers.none { it.enabled && it.verified }
}

/**
 * Backs the "Recovery Numbers" screen: unlimited add/edit/enable/disable/delete,
 * duplicate prevention via normalization, one Primary number, and verification.
 */
class RecoveryNumbersViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SecureConfigStore.getInstance(application)
    private val normalizer = PhoneNumberNormalizer()

    private val _uiState = MutableStateFlow(RecoveryNumbersUiState())
    val uiState: StateFlow<RecoveryNumbersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.recoveryNumbers.collect { numbers ->
                _uiState.value = _uiState.value.copy(numbers = numbers)
            }
        }
    }

    fun addNumber(rawNumber: String, label: String) {
        val normalized = normalizer.normalize(rawNumber)
        if (normalized == null) {
            setError("Enter a valid phone number, including country code if possible.")
            return
        }
        val finalLabel = label.ifBlank { "Recovery Number ${_uiState.value.numbers.size + 1}" }
        val result = store.addRecoveryNumber(
            RecoveryNumber(e164Number = normalized, label = finalLabel)
        )
        result.onFailure { setError(it.message ?: "This number is already saved.") }
            .onSuccess {
                setInfo("Number added. Verify it to enable recovery from this number.")
                logEvent("Recovery Number Added", finalLabel, ActivityStatus.INFO)
            }
    }

    fun updateLabel(id: String, newLabel: String) {
        val existing = _uiState.value.numbers.firstOrNull { it.id == id } ?: return
        store.updateRecoveryNumber(existing.copy(label = newLabel))
    }

    fun setEnabled(id: String, enabled: Boolean) {
        store.setEnabled(id, enabled)
        if (_uiState.value.numbers.none { (it.id == id && enabled) || (it.id != id && it.enabled) }) {
            setInfo("No active recovery number is configured.")
        }
    }

    fun setPrimary(id: String) = store.setPrimary(id)

    fun removeNumber(id: String) = store.removeRecoveryNumber(id)

    /**
     * Sends a one-time verification code style confirmation. In this local/SMS-only
     * architecture, "verify" means: send a plain SMS to the number containing a short
     * confirmation notice, and the owner confirms receipt in-app. This avoids requiring
     * any backend server while still ensuring the number is real and reachable.
     */
    fun sendVerification(id: String) {
        val number = _uiState.value.numbers.firstOrNull { it.id == id } ?: return
        RecoverySmsSender(getApplication()).sendPlainNotice(
            number.e164Number,
            "This number was just added as a recovery number for SM Find Device. " +
                "If you did not do this, remove it from the app immediately."
        )
        setInfo("Verification SMS sent. Confirm receipt to mark this number verified.")
    }

    fun confirmVerified(id: String) {
        store.markVerified(id)
        val number = _uiState.value.numbers.firstOrNull { it.id == id }
        setInfo("Number verified. It can now trigger location recovery.")
        logEvent("Recovery Number Verified", number?.label ?: "Recovery number", ActivityStatus.SUCCESS)
    }

    private fun logEvent(title: String, detail: String, status: ActivityStatus) {
        store.appendActivityEvent(
            ActivityEvent(
                id = UUID.randomUUID().toString(),
                category = ActivityCategory.RECOVERY_NUMBER,
                title = title,
                detail = detail,
                timestampMillis = System.currentTimeMillis(),
                status = status
            )
        )
    }

    /** Runs a real, owner-visible end-to-end test against this one number. */
    fun testRecovery(id: String) {
        val number = _uiState.value.numbers.firstOrNull { it.id == id } ?: return
        if (!number.enabled || !number.verified) {
            setError("Enable and verify this number before testing recovery.")
            return
        }
        LocationRecoveryService.enqueueRecoveryRequest(
            context = getApplication(),
            recipientE164 = number.e164Number,
            isTest = true
        )
        setInfo("Test recovery started for ${number.label}.")
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message, infoMessage = null)
    }

    private fun setInfo(message: String) {
        _uiState.value = _uiState.value.copy(infoMessage = message, errorMessage = null)
    }
}
