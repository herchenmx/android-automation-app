package com.herchenmx.automationapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "automation_state")

class DataStoreManager(private val context: Context) {

    companion object {
        val FOREGROUND_APP_RULE_ENABLED = booleanPreferencesKey("fg_app_rule_enabled")
        val BATTERY_RULE_ENABLED = booleanPreferencesKey("battery_rule_enabled")
        val MEDIA_BT_RULE_ENABLED = booleanPreferencesKey("media_bt_rule_enabled")
        val SAVED_VOLUME = intPreferencesKey("saved_volume_stream_music")
        val SAVED_DND_MODE = intPreferencesKey("saved_dnd_mode")
        val SAVED_WIFI_ENABLED = booleanPreferencesKey("saved_wifi_enabled")
        val SAVED_BT_ENABLED = booleanPreferencesKey("saved_bt_enabled")
        val SAVED_AUTO_SYNC = booleanPreferencesKey("saved_auto_sync")
    }

    fun getRuleEnabled(key: Preferences.Key<Boolean>): Flow<Boolean> =
        context.dataStore.data.map { it[key] ?: true }

    suspend fun setRuleEnabled(key: Preferences.Key<Boolean>, enabled: Boolean) {
        context.dataStore.edit { it[key] = enabled }
    }

    suspend fun saveInt(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    fun getInt(key: Preferences.Key<Int>, default: Int): Flow<Int> =
        context.dataStore.data.map { it[key] ?: default }

    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean): Flow<Boolean> =
        context.dataStore.data.map { it[key] ?: default }
}
