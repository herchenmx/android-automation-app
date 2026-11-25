package com.herchenmx.automationapp.service

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.herchenmx.automationapp.triggers.MediaSessionActiveTrigger
import timber.log.Timber

class AutomationNotificationListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var sessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Timber.d("NotificationListenerService: connected")
        setupMediaSessionMonitoring()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        clearCallbacks()
        currentlyPlaying.clear()
        Timber.d("NotificationListenerService: disconnected")
    }

    private fun setupMediaSessionMonitoring() {
        try {
            val component = ComponentName(this, AutomationNotificationListenerService::class.java)
            mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
            sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                Timber.d("NotificationListenerService: sessions changed count=${controllers?.size}")
                // Mark all previously tracked packages as stopped before re-scanning
                currentlyPlaying.toSet().forEach { pkg ->
                    MediaSessionActiveTrigger.notifyPlaybackChanged(pkg, false)
                }
                currentlyPlaying.clear()
                clearCallbacks()
                controllers?.forEach { attachCallback(it) }
                notifyAllPlaybackStates(controllers)
            }
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener!!, component)
            val initial = mediaSessionManager?.getActiveSessions(component)
            initial?.forEach { attachCallback(it) }
            notifyAllPlaybackStates(initial)
        } catch (e: Exception) {
            Timber.e(e, "NotificationListenerService: failed to set up media monitoring")
        }
    }

    private fun attachCallback(controller: MediaController) {
        if (controllerCallbacks.containsKey(controller)) return
        val pkg = controller.packageName
        activeControllers[pkg] = controller
        val cb = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                val isPlaying = state?.state == PlaybackState.STATE_PLAYING
                Timber.d("NotificationListenerService: $pkg isPlaying=$isPlaying")
                if (isPlaying) currentlyPlaying.add(pkg) else currentlyPlaying.remove(pkg)
                MediaSessionActiveTrigger.notifyPlaybackChanged(pkg, isPlaying)
            }
            override fun onSessionDestroyed() {
                activeControllers.remove(pkg)
            }
        }
        controller.registerCallback(cb)
        controllerCallbacks[controller] = cb
    }

    private fun clearCallbacks() {
        controllerCallbacks.forEach { (ctrl, cb) ->
            try { ctrl.unregisterCallback(cb) } catch (_: Exception) {}
        }
        controllerCallbacks.clear()
        activeControllers.clear()
    }

    private fun notifyAllPlaybackStates(controllers: List<MediaController>?) {
        controllers?.forEach { ctrl ->
            val isPlaying = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
            Timber.d("NotificationListenerService: initial ${ctrl.packageName} isPlaying=$isPlaying")
            if (isPlaying) currentlyPlaying.add(ctrl.packageName) else currentlyPlaying.remove(ctrl.packageName)
            MediaSessionActiveTrigger.notifyPlaybackChanged(ctrl.packageName, isPlaying)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    companion object {
        // Packages currently known to be playing — used by triggers to check initial state
        val currentlyPlaying: MutableSet<String> = mutableSetOf()
        // Live MediaController per package — used by PauseMediaSessionAction
        val activeControllers: MutableMap<String, MediaController> = mutableMapOf()
    }
}
