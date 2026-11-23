package com.herchenmx.automationapp.actions

import android.content.Context
import android.provider.Settings
import com.herchenmx.automationapp.engine.Action
import com.herchenmx.automationapp.engine.StateManager
import timber.log.Timber

/**
 * Saves the current value of a Settings.System integer key, sets it to [targetValue],
 * and restores the original on revert. Requires WRITE_SETTINGS permission.
 */
class SetSystemSettingAction(
    override val id: String,
    private val context: Context,
    private val stateManager: StateManager,
    private val settingName: String,
    private val targetValue: Int,
) : Action {

    override suspend fun execute() {
        try {
            val current = Settings.System.getInt(context.contentResolver, settingName)
            stateManager.save(id, current)
            Settings.System.putInt(context.contentResolver, settingName, targetValue)
            Timber.d("SetSystemSettingAction[$id]: $settingName saved=$current set=$targetValue")
        } catch (e: Exception) {
            Timber.e(e, "SetSystemSettingAction[$id]: failed to set $settingName=$targetValue")
        }
    }

    override suspend fun revert() {
        val saved = stateManager.restore<Int>(id) ?: return
        try {
            Settings.System.putInt(context.contentResolver, settingName, saved)
            Timber.d("SetSystemSettingAction[$id]: $settingName restored=$saved")
        } catch (e: Exception) {
            Timber.e(e, "SetSystemSettingAction[$id]: failed to restore $settingName=$saved")
        }
    }
}
