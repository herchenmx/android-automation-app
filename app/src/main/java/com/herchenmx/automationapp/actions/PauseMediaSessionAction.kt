package com.herchenmx.automationapp.actions

import com.herchenmx.automationapp.engine.Action
import com.herchenmx.automationapp.service.AutomationNotificationListenerService
import timber.log.Timber

/**
 * Pauses the target app's MediaSession when the rule activates, and resumes it on revert.
 * Requires the app to expose a MediaSession (Instagram, TikTok, YouTube do).
 * Does not affect other apps' audio.
 */
class PauseMediaSessionAction(
    override val id: String,
    private val targetPackage: String,
) : Action {

    override suspend fun execute() {
        val controller = AutomationNotificationListenerService.activeControllers[targetPackage]
        if (controller != null) {
            controller.transportControls.pause()
            Timber.d("PauseMediaSessionAction[$id]: paused $targetPackage")
        } else {
            Timber.w("PauseMediaSessionAction[$id]: no active session for $targetPackage")
        }
    }

    override suspend fun revert() {
        val controller = AutomationNotificationListenerService.activeControllers[targetPackage]
        if (controller != null) {
            controller.transportControls.play()
            Timber.d("PauseMediaSessionAction[$id]: resumed $targetPackage")
        } else {
            Timber.w("PauseMediaSessionAction[$id]: no active session to resume for $targetPackage")
        }
    }
}
