package com.herchenmx.automationapp.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.herchenmx.automationapp.triggers.ForegroundAppTrigger
import timber.log.Timber

class AutomationAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            Timber.d("AccessibilityService: foreground=$pkg")
            ForegroundAppTrigger.notifyForegroundChanged(pkg)
        }
    }

    override fun onInterrupt() {
        Timber.d("AccessibilityService: interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("AccessibilityService: connected")
    }
}
