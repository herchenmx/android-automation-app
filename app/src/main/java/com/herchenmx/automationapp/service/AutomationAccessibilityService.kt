package com.herchenmx.automationapp.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.herchenmx.automationapp.actions.SuppressMediaSessionAction
import com.herchenmx.automationapp.service.AutomationNotificationListenerService
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

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val active = SuppressMediaSessionAction.activePackages
            val paused = SuppressMediaSessionAction.suppressionPaused
            // Only intercept if suppression is running and not already paused
            val pkg = active.firstOrNull { !paused.contains(it) }
            if (pkg != null) {
                paused.add(pkg)
                // Resume the app's audio immediately
                AutomationNotificationListenerService.activeControllers[pkg]
                    ?.transportControls?.play()
                Timber.d("AccessibilityService: volume-up paused suppression for $pkg")
                return false // still let the volume change go through
            }
        }
        return false
    }

    override fun onInterrupt() {
        Timber.d("AccessibilityService: interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("AccessibilityService: connected")
    }
}
