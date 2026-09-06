package com.sahin.smfinddevice.data

import kotlinx.serialization.Serializable

@Serializable
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
    val provider: String,
    val isFresh: Boolean
) {
    fun mapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"

    fun accuracyLabel(): String = "approximately ${accuracyMeters.toInt()} meters"
}

sealed class LocationOutcome {
    data class Success(val result: LocationResult) : LocationOutcome()
    data class FallbackToLastKnown(val result: LocationResult) : LocationOutcome()
    data class Failure(val reason: LocationFailureReason) : LocationOutcome()
}

enum class LocationFailureReason {
    LOCATION_PERMISSION_DENIED,
    PRECISE_LOCATION_DENIED,
    GPS_DISABLED,
    LOCATION_SERVICES_DISABLED,
    SIGNAL_UNAVAILABLE,
    TIMEOUT,
    NO_LAST_KNOWN_LOCATION,
    AIRPLANE_MODE,
    UNKNOWN
}

enum class ProtectionState {
    PROTECTION_ACTIVE,
    ACTION_REQUIRED,
    LIMITED_PROTECTION,
    PROTECTION_DISABLED
}
