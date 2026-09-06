package com.sahin.smfinddevice.data

import kotlinx.serialization.Serializable

/**
 * A single real, owner-visible event for the Activity screen timeline.
 *
 * These are only ever appended by real app actions (a test run, a permission
 * check, an actual authorized SMS recovery, a protection toggle) — never
 * synthesized for display purposes. See [com.sahin.smfinddevice.storage.SecureConfigStore].
 */
@Serializable
data class ActivityEvent(
    val id: String,
    val category: ActivityCategory,
    val title: String,
    val detail: String,
    val timestampMillis: Long,
    val status: ActivityStatus,
    val accuracyMeters: Float? = null
)

enum class ActivityCategory {
    RECOVERY_TEST,
    SMS_RECOVERY,
    LOCATION_REQUEST,
    PERMISSION,
    PROTECTION,
    RECOVERY_NUMBER,
    COMMANDS
}

enum class ActivityStatus {
    SUCCESS,
    WARNING,
    FAILURE,
    INFO
}

/** Bounded so encrypted-prefs storage never grows without limit. */
const val MAX_ACTIVITY_EVENTS = 50
