package com.sahin.smfinddevice.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A single trusted recovery number.
 *
 * Only numbers with [enabled] == true and [verified] == true are permitted to
 * trigger a location recovery response. Everything about a number (the raw
 * digits) is treated as sensitive and is persisted only via
 * [com.sahin.smfinddevice.storage.SecureConfigStore], never logged.
 */
@Serializable
data class RecoveryNumber(
    val id: String = UUID.randomUUID().toString(),
    val e164Number: String,
    val label: String,
    val enabled: Boolean = true,
    val verified: Boolean = false,
    val isPrimary: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
) {
    /** Digits-only, last 4, safe to show in a log line or UI subtitle. */
    fun maskedDisplay(): String {
        val digits = e164Number.filter { it.isDigit() }
        val tail = digits.takeLast(4)
        return "•••• $tail"
    }
}

enum class NumberVerificationState {
    UNVERIFIED,
    PENDING,
    VERIFIED
}
