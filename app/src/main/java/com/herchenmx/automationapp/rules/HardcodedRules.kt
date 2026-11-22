package com.herchenmx.automationapp.rules

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.herchenmx.automationapp.actions.DndModeAction
import com.herchenmx.automationapp.actions.SetVolumeAction
import com.herchenmx.automationapp.actions.SystemFeature
import com.herchenmx.automationapp.actions.SystemToggleAction
import com.herchenmx.automationapp.engine.Rule
import com.herchenmx.automationapp.engine.StateManager
import com.herchenmx.automationapp.triggers.BatteryLevelTrigger
import com.herchenmx.automationapp.triggers.BluetoothAudioTrigger
import com.herchenmx.automationapp.triggers.ForegroundAppTrigger
import com.herchenmx.automationapp.triggers.MediaSessionActiveTrigger

object HardcodedRules {

    // Package to silence when it enters the foreground — change to your target app.
    private const val TARGET_APP_PACKAGE = "com.example.targetapp"
    private const val BATTERY_THRESHOLD_PERCENT = 50

    fun buildRules(context: Context, stateManager: StateManager): List<Rule> = listOf(

        // Rule 1: When TARGET_APP_PACKAGE comes to the foreground, mute speaker volume.
        // Volume is restored to whatever it was when the app leaves the foreground.
        Rule(
            id = "foreground_app_volume",
            name = "Mute app audio on foreground",
            triggers = listOf(
                ForegroundAppTrigger(id = "fg_app_trigger", targetPackage = TARGET_APP_PACKAGE)
            ),
            actions = listOf(
                SetVolumeAction(
                    id = "mute_music",
                    context = context,
                    stateManager = stateManager,
                    streamType = AudioManager.STREAM_MUSIC,
                    targetVolume = 0,
                )
            ),
        ),

        // Rule 2: When battery drops below threshold, disable auto-sync and dim brightness.
        // State is restored when battery rises above threshold.
        Rule(
            id = "battery_saver",
            name = "Battery saver at $BATTERY_THRESHOLD_PERCENT%",
            triggers = listOf(
                BatteryLevelTrigger(
                    id = "battery_trigger",
                    context = context,
                    thresholdPercent = BATTERY_THRESHOLD_PERCENT,
                )
            ),
            actions = listOf(
                SystemToggleAction(
                    id = "disable_sync",
                    context = context,
                    stateManager = stateManager,
                    feature = SystemFeature.AUTO_SYNC,
                    enabledWhenActive = false,
                ),
                SystemToggleAction(
                    id = "manual_brightness",
                    context = context,
                    stateManager = stateManager,
                    feature = SystemFeature.SCREEN_BRIGHTNESS,
                    enabledWhenActive = false,
                ),
            ),
        ),

        // Rule 3: When YT Music is playing AND a Bluetooth audio device is connected,
        // enable DND (priority only). Both triggers must be active simultaneously.
        Rule(
            id = "yt_music_bt_dnd",
            name = "DND while YT Music on Bluetooth",
            triggers = listOf(
                MediaSessionActiveTrigger(id = "yt_music_trigger"),
                BluetoothAudioTrigger(id = "bt_audio_trigger", context = context),
            ),
            actions = listOf(
                DndModeAction(
                    id = "enable_dnd",
                    context = context,
                    stateManager = stateManager,
                    dndMode = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                )
            ),
        ),
    )
}
