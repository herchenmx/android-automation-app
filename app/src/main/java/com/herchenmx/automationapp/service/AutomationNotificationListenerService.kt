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

    private val ytMusicPackage = "com.google.android.apps.youtube.music"
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
        Timber.d("NotificationListenerService: disconnected")
    }

    private fun setupMediaSessionMonitoring() {
        try {
            val component = ComponentName(this, AutomationNotificationListenerService::class.java)
            mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
            sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                Timber.d("NotificationListenerService: sessions changed count=${controllers?.size}")
                clearCallbacks()
                controllers?.forEach { attachCallback(it) }
                checkYtMusicPlayback(controllers)
            }
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener!!, component)
            val initial = mediaSessionManager?.getActiveSessions(component)
            initial?.forEach { attachCallback(it) }
            checkYtMusicPlayback(initial)
        } catch (e: Exception) {
            Timber.e(e, "NotificationListenerService: failed to set up media monitoring")
        }
    }

    private fun attachCallback(controller: MediaController) {
        if (controllerCallbacks.containsKey(controller)) return
        val cb = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                if (controller.packageName == ytMusicPackage) {
                    val isPlaying = state?.state == PlaybackState.STATE_PLAYING
                    Timber.d("NotificationListenerService: YT Music isPlaying=$isPlaying")
                    MediaSessionActiveTrigger.notifyPlaybackChanged(ytMusicPackage, isPlaying)
                }
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
    }

    private fun checkYtMusicPlayback(controllers: List<MediaController>?) {
        val isPlaying = controllers?.any { ctrl ->
            ctrl.packageName == ytMusicPackage &&
                ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: false
        MediaSessionActiveTrigger.notifyPlaybackChanged(ytMusicPackage, isPlaying)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
