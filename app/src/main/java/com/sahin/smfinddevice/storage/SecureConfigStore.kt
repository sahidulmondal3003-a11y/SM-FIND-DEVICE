package com.sahin.smfinddevice.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sahin.smfinddevice.data.ActivityEvent
import com.sahin.smfinddevice.data.LocationResult
import com.sahin.smfinddevice.data.MAX_ACTIVITY_EVENTS
import com.sahin.smfinddevice.data.RecoveryCommand
import com.sahin.smfinddevice.data.RecoveryNumber
import com.sahin.smfinddevice.data.defaultRecoveryCommands
import com.sahin.smfinddevice.utils.SafeLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * All recovery-relevant configuration (trusted numbers, commands, cooldown state,
 * protection toggle) lives here, backed by Android Keystore-encrypted SharedPreferences.
 *
 * Nothing in this file is ever written to Logcat: see [com.sahin.smfinddevice.utils.SafeLog].
 */
class SecureConfigStore private constructor(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val appContext = context.applicationContext

    /**
     * Encrypted prefs are preferred, but Keystore/Tink initialization can fail on some
     * emulator images or OEM builds. If it does, fall back to plain SharedPreferences
     * rather than crashing the whole app on launch — the owner can still use the app,
     * and this is far safer than the app simply refusing to open.
     */
    private val prefs: SharedPreferences by lazy { buildPrefs() }

    private fun buildPrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                "sm_find_device_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            SafeLog.e("Encrypted storage init failed, falling back to standard storage", e)
            appContext.getSharedPreferences("sm_find_device_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    // ---------------------------------------------------------------------
    // Recovery numbers
    // ---------------------------------------------------------------------

    private val _recoveryNumbers = MutableStateFlow<List<RecoveryNumber>>(emptyList())
    val recoveryNumbers: StateFlow<List<RecoveryNumber>> = _recoveryNumbers.asStateFlow()

    fun loadAll() {
        try {
            _recoveryNumbers.value = readRecoveryNumbers()
            if (readCommands().isEmpty()) {
                writeCommands(defaultRecoveryCommands())
            }
        } catch (e: Exception) {
            SafeLog.e("loadAll() failed; starting with empty configuration", e)
        }
    }

    private fun readRecoveryNumbers(): List<RecoveryNumber> {
        val raw = prefs.getString(KEY_RECOVERY_NUMBERS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<RecoveryNumber>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeRecoveryNumbers(numbers: List<RecoveryNumber>) {
        try {
            prefs.edit().putString(KEY_RECOVERY_NUMBERS, json.encodeToString(numbers)).apply()
            _recoveryNumbers.value = numbers
        } catch (e: Exception) {
            SafeLog.e("Failed to persist recovery numbers", e)
        }
    }

    fun addRecoveryNumber(number: RecoveryNumber): Result<Unit> {
        val current = readRecoveryNumbers()
        val duplicate = current.any { it.e164Number == number.e164Number }
        if (duplicate) return Result.failure(IllegalArgumentException("This number is already saved."))

        val updated = if (current.isEmpty()) {
            // First number added automatically becomes primary.
            current + number.copy(isPrimary = true)
        } else {
            current + number
        }
        writeRecoveryNumbers(updated)
        return Result.success(Unit)
    }

    fun updateRecoveryNumber(updated: RecoveryNumber) {
        val current = readRecoveryNumbers()
        writeRecoveryNumbers(current.map { if (it.id == updated.id) updated else it })
    }

    fun removeRecoveryNumber(id: String) {
        val current = readRecoveryNumbers()
        val removedWasPrimary = current.firstOrNull { it.id == id }?.isPrimary == true
        var updated = current.filterNot { it.id == id }
        if (removedWasPrimary && updated.isNotEmpty()) {
            updated = updated.mapIndexed { index, n -> if (index == 0) n.copy(isPrimary = true) else n }
        }
        writeRecoveryNumbers(updated)
    }

    fun setPrimary(id: String) {
        val current = readRecoveryNumbers()
        writeRecoveryNumbers(current.map { it.copy(isPrimary = it.id == id) })
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val current = readRecoveryNumbers()
        writeRecoveryNumbers(current.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun markVerified(id: String) {
        val current = readRecoveryNumbers()
        writeRecoveryNumbers(current.map { if (it.id == id) it.copy(verified = true) else it })
    }

    fun markUsedNow(id: String) {
        val current = readRecoveryNumbers()
        writeRecoveryNumbers(
            current.map { if (it.id == id) it.copy(lastUsedAt = System.currentTimeMillis()) else it }
        )
    }

    fun hasAnyActiveRecoveryNumber(): Boolean =
        readRecoveryNumbers().any { it.enabled && it.verified }

    /** Finds the enabled+verified stored number matching an inbound SMS sender address, if any. */
    fun findAuthorizedNumberForSender(
        senderAddress: String,
        normalizer: com.sahin.smfinddevice.security.PhoneNumberNormalizer
    ): RecoveryNumber? =
        readRecoveryNumbers().firstOrNull {
            it.enabled && it.verified && normalizer.matchesSender(senderAddress, it.e164Number)
        }

    // ---------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------

    private val _commands = MutableStateFlow<List<RecoveryCommand>>(emptyList())
    val commands: StateFlow<List<RecoveryCommand>> = _commands.asStateFlow()

    fun readCommands(): List<RecoveryCommand> {
        val raw = prefs.getString(KEY_COMMANDS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<RecoveryCommand>>(raw).also { _commands.value = it }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun writeCommands(commands: List<RecoveryCommand>) {
        try {
            val capped = commands.take(com.sahin.smfinddevice.data.MAX_RECOVERY_COMMAND_SLOTS)
            prefs.edit().putString(KEY_COMMANDS, json.encodeToString(capped)).apply()
            _commands.value = capped
        } catch (e: Exception) {
            SafeLog.e("Failed to persist commands", e)
        }
    }

    fun enabledCommands(): List<RecoveryCommand> = readCommands().filter { it.enabled }

    // ---------------------------------------------------------------------
    // Protection toggle + cooldown bookkeeping
    // ---------------------------------------------------------------------

    var protectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()

    var lastRecoveryRequestAt: Long
        get() = prefs.getLong(KEY_LAST_REQUEST_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_REQUEST_AT, value).apply()

    var lastLocationResultJson: String?
        get() = prefs.getString(KEY_LAST_LOCATION, null)
        set(value) = prefs.edit().putString(KEY_LAST_LOCATION, value).apply()

    /** Convenience accessor used by the Dashboard's Device Recovery Summary — real data only. */
    fun lastLocationResult(): LocationResult? {
        val raw = lastLocationResultJson ?: return null
        return try {
            json.decodeFromString<LocationResult>(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun rememberLastLocationResult(result: LocationResult) {
        try {
            lastLocationResultJson = json.encodeToString(result)
        } catch (e: Exception) {
            SafeLog.e("Failed to persist last location result", e)
        }
    }

    var lastSuccessfulTestAt: Long
        get() = prefs.getLong(KEY_LAST_TEST_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_TEST_AT, value).apply()

    // ---------------------------------------------------------------------
    // Activity log — real, owner-visible events only (see ActivityEvent).
    // ---------------------------------------------------------------------

    private val _activityEvents = MutableStateFlow<List<ActivityEvent>>(readActivityEvents())
    val activityEvents: StateFlow<List<ActivityEvent>> = _activityEvents.asStateFlow()

    private fun readActivityEvents(): List<ActivityEvent> {
        val raw = prefs.getString(KEY_ACTIVITY_EVENTS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ActivityEvent>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun appendActivityEvent(event: ActivityEvent) {
        try {
            val updated = (listOf(event) + readActivityEvents()).take(MAX_ACTIVITY_EVENTS)
            prefs.edit().putString(KEY_ACTIVITY_EVENTS, json.encodeToString(updated)).apply()
            _activityEvents.value = updated
        } catch (e: Exception) {
            SafeLog.e("Failed to persist activity event", e)
        }
    }

    /** De-duplicates redelivered SMS broadcasts (same sender+body+minute). */
    fun hasSeenRequestId(requestId: String): Boolean =
        prefs.getStringSet(KEY_SEEN_REQUEST_IDS, emptySet())?.contains(requestId) == true

    fun rememberRequestId(requestId: String) {
        val current = prefs.getStringSet(KEY_SEEN_REQUEST_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(requestId)
        // Keep this bounded; only the most recent handful matter for loop/duplicate prevention.
        val trimmed = if (current.size > 20) current.toList().takeLast(20).toMutableSet() else current
        prefs.edit().putStringSet(KEY_SEEN_REQUEST_IDS, trimmed).apply()
    }

    companion object {
        private const val KEY_RECOVERY_NUMBERS = "recovery_numbers"
        private const val KEY_COMMANDS = "recovery_commands"
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_LAST_REQUEST_AT = "last_request_at"
        private const val KEY_LAST_LOCATION = "last_location_result"
        private const val KEY_SEEN_REQUEST_IDS = "seen_request_ids"
        private const val KEY_LAST_TEST_AT = "last_successful_test_at"
        private const val KEY_ACTIVITY_EVENTS = "activity_events"

        @Volatile private var instance: SecureConfigStore? = null

        fun getInstance(context: Context): SecureConfigStore =
            instance ?: synchronized(this) {
                instance ?: SecureConfigStore(context.applicationContext).also {
                    try {
                        it.loadAll()
                    } catch (e: Exception) {
                        SafeLog.e("SecureConfigStore init failed", e)
                    }
                    instance = it
                }
            }
    }
}
