package com.sahin.smfinddevice

import android.app.Application
import com.sahin.smfinddevice.storage.SecureConfigStore

class SmFindDeviceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Warm the encrypted store once at process start; cheap and avoids first-frame jank.
        SecureConfigStore.getInstance(this)
    }
}
