package com.herchenmx.automationapp.rules

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import com.herchenmx.automationapp.actions.DndModeAction
import com.herchenmx.automationapp.actions.PauseMediaSessionAction
import com.herchenmx.automationapp.actions.SetSystemSettingAction
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
import kotlin.math.roundToInt

object HardcodedRules {

    fun buildRules(context: Context, stateManager: StateManager): List<Rule> {
        val targetApp = AppPreferences.getTargetApp(context)
        val batteryThreshold = AppPreferences.getBatteryThreshold(context)
        val btAddresses = AppPreferences.getBluetoothTargetDevices(context).map { it.address }.toSet()
        val dndApps = AppPreferences.getDndTargetApps(context).toSet()

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val batteryActions = buildList {
            if (AppPreferences.getBatterySaverAutoSync(context)) {
                add(SystemToggleAction(
                    id = "bs_sync", context = context, stateManager = stateManager,
                    feature = SystemFeature.AUTO_SYNC, enabledWhenActive = false,
                ))
            }
            if (AppPreferences.getBatterySaverBrightnessMode(context)) {
                add(SystemToggleAction(
                    id = "bs_brightness_mode", context = context, stateManager = stateManager,
                    feature = SystemFeature.SCREEN_BRIGHTNESS, enabledWhenActive = false,
                ))
            }
            if (AppPreferences.getBatterySaverBrightnessLevel(context)) {
                val pct = AppPreferences.getBatterySaverBrightnessLevelPct(context)
                val level = (pct * 255 / 100.0).roundToInt().coerceIn(1, 255)
                add(SetSystemSettingAction(
                    id = "bs_brightness_level", context = context, stateManager = stateManager,
                    settingName = Settings.System.SCREEN_BRIGHTNESS, targetValue = level,
                ))
            }
            if (AppPreferences.getBatterySaverTimeout(context)) {
                val timeoutMs = AppPreferences.getBatterySaverTimeoutSecs(context) * 1000
                add(SetSystemSettingAction(
                    id = "bs_screen_timeout", context = context, stateManager = stateManager,
                    settingName = Settings.System.SCREEN_OFF_TIMEOUT, targetValue = timeoutMs,
                ))
            }
            if (AppPreferences.getBatterySaverDnd(context)) {
                add(DndModeAction(
                    id = "bs_dnd", context = context, stateManager = stateManager,
                    dndMode = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                ))
            }
            if (AppPreferences.getBatterySaverMediaVolume(context)) {
                val pct = AppPreferences.getBatterySaverMediaVolumePct(context)
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (maxVol * pct / 100.0).roundToInt()
                add(SetVolumeAction(
                    id = "bs_media_vol", context = context, stateManager = stateManager,
                    streamType = AudioManager.STREAM_MUSIC, targetVolume = targetVol,
                ))
            }
        }

        return listOf(
            Rule(
                id = "foreground_app_volume",
                name = "Pause app audio on foreground",
                triggers = listOf(ForegroundAppTrigger(id = "fg_app", targetPackage = targetApp)),
                actions = listOf(
                    PauseMediaSessionAction(id = "pause_fg_app", targetPackage = targetApp)
                ),
            ),
            Rule(
                id = "battery_saver",
                name = "Battery saver at $batteryThreshold%",
                triggers = listOf(BatteryLevelTrigger(
                    id = "battery_trigger", context = context, thresholdPercent = batteryThreshold,
                )),
                actions = batteryActions,
            ),
            Rule(
                id = "yt_music_bt_dnd",
                name = "DND while YT Music on Bluetooth",
                triggers = listOf(
                    MediaSessionActiveTrigger(id = "yt_music", targetPackages = dndApps),
                    BluetoothAudioTrigger(
                        id = "bt_audio", context = context, targetDeviceAddresses = btAddresses,
                    ),
                ),
                actions = listOf(DndModeAction(
                    id = "dnd", context = context, stateManager = stateManager,
                    dndMode = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                )),
            ),
        )
    }
}
