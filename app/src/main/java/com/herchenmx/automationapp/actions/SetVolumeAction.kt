package com.herchenmx.automationapp.actions

import android.content.Context
import android.media.AudioManager
import com.herchenmx.automationapp.engine.Action
import com.herchenmx.automationapp.engine.StateManager
import timber.log.Timber

class SetVolumeAction(
    override val id: String,
    private val context: Context,
    private val stateManager: StateManager,
    private val streamType: Int = AudioManager.STREAM_MUSIC,
    private val targetVolume: Int = 0,
) : Action {

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val stateKey = "volume_stream_$streamType"

    override suspend fun execute() {
        val current = audioManager.getStreamVolume(streamType)
        stateManager.save(stateKey, current)
        audioManager.setStreamVolume(streamType, targetVolume, 0)
        Timber.d("SetVolumeAction[$id]: saved=$current set=$targetVolume stream=$streamType")
    }

    override suspend fun revert() {
        val saved = stateManager.restore<Int>(stateKey)
        if (saved != null) {
            audioManager.setStreamVolume(streamType, saved, 0)
            Timber.d("SetVolumeAction[$id]: restored volume=$saved stream=$streamType")
        } else {
            Timber.w("SetVolumeAction[$id]: no saved volume to restore")
        }
    }
}
