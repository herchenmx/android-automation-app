package com.herchenmx.automationapp.triggers

import com.herchenmx.automationapp.engine.Trigger
import timber.log.Timber

/**
 * Fires when ANY of the target packages has an active (playing) media session.
 * Empty set defaults to YT Music only. State is pushed from
 * AutomationNotificationListenerService via the companion object.
 */
class MediaSessionActiveTrigger(
    override val id: String,
    val targetPackages: Set<String> = setOf("com.google.android.apps.youtube.music"),
) : Trigger {

    private var callback: ((Boolean) -> Unit)? = null
    private val playingPackages = mutableSetOf<String>()

    override fun start(onStateChange: (Boolean) -> Unit) {
        callback = onStateChange
        targetPackages.forEach { ActiveMediaListeners.register(it, this) }
        Timber.d("MediaSessionActiveTrigger[$id]: watching $targetPackages")
    }

    override fun stop() {
        targetPackages.forEach { ActiveMediaListeners.unregister(it, this) }
        playingPackages.clear()
        callback = null
        Timber.d("MediaSessionActiveTrigger[$id]: stopped")
    }

    internal fun onPlaybackChanged(pkg: String, isPlaying: Boolean) {
        val wasActive = playingPackages.isNotEmpty()
        if (isPlaying) playingPackages.add(pkg) else playingPackages.remove(pkg)
        val isActive = playingPackages.isNotEmpty()
        if (isActive != wasActive) {
            Timber.d("MediaSessionActiveTrigger[$id]: active=$isActive (pkg=$pkg playing=$isPlaying)")
            callback?.invoke(isActive)
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
            listeners[packageName]?.forEach { it.onPlaybackChanged(packageName, isPlaying) }
        }
    }
}
