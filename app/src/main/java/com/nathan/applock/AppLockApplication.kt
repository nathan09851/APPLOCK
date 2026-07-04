package com.nathan.applock

import android.app.Application
import com.nathan.applock.data.AppLockerModule

class AppLockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            AppLockerModule.init(this)
        } catch (_: Throwable) {}
    }
}
