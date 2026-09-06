package com.sahin.smfinddevice.utils

import android.util.Log
import com.sahin.smfinddevice.BuildConfig

/**
 * Centralized logging. Enforces section 30 of the spec:
 * never log recovery numbers, SMS contents, or full coordinates,
 * and never log anything at all in release builds.
 */
object SafeLog {
    private const val TAG = "SMFindDevice"

    fun d(message: String) {
        if (BuildConfig.DEBUG_LOGGING) Log.d(TAG, message)
    }

    fun w(message: String) {
        if (BuildConfig.DEBUG_LOGGING) Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG_LOGGING) Log.e(TAG, message, throwable)
    }

    /** Use this instead of the raw number anywhere a log line needs to reference "which number". */
    fun maskNumber(e164: String): String {
        val digits = e164.filter { it.isDigit() }
        return "•••${digits.takeLast(4)}"
    }
}
