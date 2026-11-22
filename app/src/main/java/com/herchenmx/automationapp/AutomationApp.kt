package com.herchenmx.automationapp

import android.app.Application
import timber.log.Timber

class AutomationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
