package com.sahin.smfinddevice.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sahin.smfinddevice.data.LocationFailureReason
import com.sahin.smfinddevice.data.LocationOutcome
import com.sahin.smfinddevice.data.LocationResult
import com.sahin.smfinddevice.permissions.PermissionChecker
import com.sahin.smfinddevice.utils.SafeLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Real, non-mocked location acquisition (spec sections 7-13, 36, 37).
 * Always requests a fresh fix first; falls back to last-known only when a fresh
 * fix cannot be obtained within [FRESH_FIX_TIMEOUT_MS], and always labels that
 * fallback as "last known", never as current.
 */
class LocationAcquisitionEngine(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val permissionChecker = PermissionChecker(context)

    companion object {
        private const val FRESH_FIX_TIMEOUT_MS = 25_000L
    }

    suspend fun acquireLocation(): LocationOutcome {
        if (!permissionChecker.hasAnyLocationPermission()) {
            return LocationOutcome.Failure(LocationFailureReason.LOCATION_PERMISSION_DENIED)
        }

        val locationManager = context.getSystemService<LocationManager>()
        val locationServicesOn = locationManager?.isLocationEnabled == true
        if (!locationServicesOn) {
            return LocationOutcome.Failure(LocationFailureReason.LOCATION_SERVICES_DISABLED)
        }

        val fresh = withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) { requestFreshFix() }
        if (fresh != null) {
            return LocationOutcome.Success(fresh)
        }

        SafeLog.d("Fresh fix timed out; attempting last-known fallback.")
        val lastKnown = requestLastKnown()
        return if (lastKnown != null) {
            LocationOutcome.FallbackToLastKnown(lastKnown)
        } else {
            LocationOutcome.Failure(LocationFailureReason.TIMEOUT)
        }
    }

    /** Used by "Test Location" — identical engine, just not tied to an SMS reply. */
    suspend fun acquireLocationForTest(): LocationOutcome = acquireLocation()

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshFix(): LocationResult? = suspendCancellableCoroutine { cont ->
        if (!permissionChecker.hasAnyLocationPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val priority = if (permissionChecker.hasPreciseLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setDurationMillis(FRESH_FIX_TIMEOUT_MS)
            .build()

        val cancellationSignal = CancellationSignal()
        cont.invokeOnCancellation { cancellationSignal.cancel() }

        fusedClient.getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    cont.resume(null)
                } else {
                    cont.resume(
                        LocationResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            timestampMillis = location.time,
                            provider = location.provider ?: "fused",
                            isFresh = true
                        )
                    )
                }
            }
            .addOnFailureListener {
                SafeLog.e("Fresh location request failed", it)
                cont.resume(null)
            }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastKnown(): LocationResult? = suspendCancellableCoroutine { cont ->
        if (!permissionChecker.hasAnyLocationPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    cont.resume(null)
                } else {
                    cont.resume(
                        LocationResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            timestampMillis = location.time,
                            provider = location.provider ?: "fused",
                            isFresh = false
                        )
                    )
                }
            }
            .addOnFailureListener { cont.resume(null) }
    }
}
