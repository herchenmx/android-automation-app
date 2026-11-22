package com.herchenmx.automationapp.actions

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.herchenmx.automationapp.engine.Action
import com.herchenmx.automationapp.engine.StateManager
import timber.log.Timber

enum class SystemFeature { WIFI, BLUETOOTH, AUTO_SYNC, SCREEN_BRIGHTNESS }

class SystemToggleAction(
    override val id: String,
    private val context: Context,
    private val stateManager: StateManager,
    private val feature: SystemFeature,
    private val enabledWhenActive: Boolean = false,
) : Action {

    private val stateKey = "system_toggle_$feature"

    override suspend fun execute() {
        when (feature) {
            // NOTE: Wi-Fi toggle via WifiManager is blocked on API 29+. Left in place for
            // reference; on modern devices this no-ops unless granted as a system app.
            SystemFeature.WIFI -> {
                Timber.d("SystemToggleAction[$id]: WIFI toggle not supported on API 29+ without system privilege")
            }
            // NOTE: BluetoothAdapter.disable() is blocked on API 33+ without system privilege.
            SystemFeature.BLUETOOTH -> {
                Timber.d("SystemToggleAction[$id]: BT toggle not supported on API 33+ without system privilege")
            }
            SystemFeature.AUTO_SYNC -> {
                val current = ContentResolver.getMasterSyncAutomatically()
                stateManager.save(stateKey, current)
                ContentResolver.setMasterSyncAutomatically(enabledWhenActive)
                Timber.d("SystemToggleAction[$id]: AutoSync saved=$current set=$enabledWhenActive")
            }
            SystemFeature.SCREEN_BRIGHTNESS -> {
                try {
                    val current = Settings.System.getInt(
                        context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE
                    )
                    stateManager.save(stateKey, current)
                    val newMode = if (enabledWhenActive)
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    else
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, newMode)
                    Timber.d("SystemToggleAction[$id]: BrightnessMode saved=$current set=$newMode")
                } catch (e: Exception) {
                    Timber.e(e, "SystemToggleAction[$id]: failed to toggle brightness mode")
                }
            }
        }
    }

    override suspend fun revert() {
        when (feature) {
            SystemFeature.WIFI -> {}
            SystemFeature.BLUETOOTH -> {}
            SystemFeature.AUTO_SYNC -> {
                val saved = stateManager.restore<Boolean>(stateKey) ?: return
                ContentResolver.setMasterSyncAutomatically(saved)
                Timber.d("SystemToggleAction[$id]: AutoSync restored=$saved")
            }
            SystemFeature.SCREEN_BRIGHTNESS -> {
                val saved = stateManager.restore<Int>(stateKey) ?: return
                try {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, saved)
                    Timber.d("SystemToggleAction[$id]: BrightnessMode restored=$saved")
                } catch (e: Exception) {
                    Timber.e(e, "SystemToggleAction[$id]: failed to restore brightness mode")
                }
            }
        }
    }
}
