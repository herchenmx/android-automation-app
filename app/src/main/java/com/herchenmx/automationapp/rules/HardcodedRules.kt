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
import com.herchenmx.automationapp.util.AppPreferences

object HardcodedRules {

    fun buildRules(context: Context, stateManager: StateManager): List<Rule> {
        val targetApp = AppPreferences.getTargetApp(context)
        val batteryThreshold = AppPreferences.getBatteryThreshold(context)
        val btAddress = AppPreferences.getBluetoothTargetAddress(context)

        val batteryActions = buildList {
            if (AppPreferences.getBatterySaverAutoSync(context)) {
                add(SystemToggleAction(
                    id = "disable_sync",
                    context = context,
                    stateManager = stateManager,
                    feature = SystemFeature.AUTO_SYNC,
                    enabledWhenActive = false,
                ))
            }
            if (AppPreferences.getBatterySaverBrightness(context)) {
                add(SystemToggleAction(
                    id = "manual_brightness",
                    context = context,
                    stateManager = stateManager,
                    feature = SystemFeature.SCREEN_BRIGHTNESS,
                    enabledWhenActive = false,
                ))
            }
        }

        return listOf(
            Rule(
                id = "foreground_app_volume",
                name = "Mute app audio on foreground",
                triggers = listOf(
                    ForegroundAppTrigger(id = "fg_app_trigger", targetPackage = targetApp)
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
            Rule(
                id = "battery_saver",
                name = "Battery saver at $batteryThreshold%",
                triggers = listOf(
                    BatteryLevelTrigger(
                        id = "battery_trigger",
                        context = context,
                        thresholdPercent = batteryThreshold,
                    )
                ),
                actions = batteryActions,
            ),
            Rule(
                id = "yt_music_bt_dnd",
                name = "DND while YT Music on Bluetooth",
                triggers = listOf(
                    MediaSessionActiveTrigger(id = "yt_music_trigger"),
                    BluetoothAudioTrigger(
                        id = "bt_audio_trigger",
                        context = context,
                        targetDeviceAddress = btAddress,
                    ),
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
}
