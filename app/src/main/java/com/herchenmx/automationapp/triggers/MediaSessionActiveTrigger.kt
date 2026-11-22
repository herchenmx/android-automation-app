package com.herchenmx.automationapp.triggers

import com.herchenmx.automationapp.engine.Trigger
import timber.log.Timber

/**
 * Fires when the target package has an active (playing) media session.
 * State is pushed from AutomationNotificationListenerService via the companion object.
 */
class MediaSessionActiveTrigger(
    override val id: String,
    val targetPackage: String = "com.google.android.apps.youtube.music",
) : Trigger {

    private var callback: ((Boolean) -> Unit)? = null
    private var currentlyActive = false

    override fun start(onStateChange: (Boolean) -> Unit) {
        callback = onStateChange
        ActiveMediaListeners.register(targetPackage, this)
        Timber.d("MediaSessionActiveTrigger[$id]: watching $targetPackage")
    }

    override fun stop() {
        ActiveMediaListeners.unregister(targetPackage, this)
        callback = null
        Timber.d("MediaSessionActiveTrigger[$id]: stopped")
    }

    internal fun onPlaybackChanged(isPlaying: Boolean) {
        if (isPlaying != currentlyActive) {
            currentlyActive = isPlaying
            Timber.d("MediaSessionActiveTrigger[$id]: isPlaying=$isPlaying")
            callback?.invoke(isPlaying)
        }
    }

    companion object ActiveMediaListeners {
        private val listeners = mutableMapOf<String, MutableList<MediaSessionActiveTrigger>>()

        fun register(pkg: String, trigger: MediaSessionActiveTrigger) {
            listeners.getOrPut(pkg) { mutableListOf() }.add(trigger)
        }

        fun unregister(pkg: String, trigger: MediaSessionActiveTrigger) {
            listeners[pkg]?.remove(trigger)
        }

        fun notifyPlaybackChanged(packageName: String, isPlaying: Boolean) {
            listeners[packageName]?.forEach { it.onPlaybackChanged(isPlaying) }
        }
    }
}
