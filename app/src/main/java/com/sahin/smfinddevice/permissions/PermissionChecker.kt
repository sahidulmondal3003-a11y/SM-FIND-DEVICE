package com.sahin.smfinddevice.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionChecker(private val context: Context) {

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasPreciseLocationPermission(): Boolean = granted(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasApproximateLocationPermission(): Boolean = granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasAnyLocationPermission(): Boolean = hasPreciseLocationPermission() || hasApproximateLocationPermission()

    fun hasBackgroundLocationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            hasAnyLocationPermission() // no separate background permission before Q
        }

    fun hasSendSmsPermission(): Boolean = granted(Manifest.permission.SEND_SMS)

    fun hasReceiveSmsPermission(): Boolean = granted(Manifest.permission.RECEIVE_SMS)

    fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // not required before Android 13
        }

    fun requiredPermissionsForSetup(): List<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }
        return list
    }

    /** Requested as a separate, later step per Android's background-location UX rules. */
    fun backgroundLocationPermission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else null
}
