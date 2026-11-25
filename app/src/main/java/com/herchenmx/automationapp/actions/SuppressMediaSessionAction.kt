package com.herchenmx.automationapp.actions

import android.media.session.PlaybackState
import com.herchenmx.automationapp.engine.Action
import com.herchenmx.automationapp.service.AutomationNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * While active, polls the target app's MediaSession every 300ms and pauses it
 * whenever it is playing. Stops when reverted (app leaves foreground).
 *
 * This suppresses all audio from the target app without affecting other apps.
 */
class SuppressMediaSessionAction(
    override val id: String,
    private val targetPackage: String,
) : Action {

    private var job: Job? = null

    override suspend fun execute() {
        Timber.d("SuppressMediaSessionAction[$id]: starting suppression for $targetPackage")
        job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                val controller = AutomationNotificationListenerService.activeControllers[targetPackage]
                if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                    Timber.d("SuppressMediaSessionAction[$id]: paused $targetPackage")
                }
                delay(300L)
            }
        }
    }

    override suspend fun revert() {
        job?.cancel()
        job = null
        Timber.d("SuppressMediaSessionAction[$id]: suppression stopped for $targetPackage")
    }
}
