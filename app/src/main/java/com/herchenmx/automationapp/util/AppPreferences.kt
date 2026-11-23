package com.herchenmx.automationapp.util

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "automation_config"

    // Rule 1: target app package
    fun getTargetApp(context: Context): String = prefs(context).getString("target_app_package", "") ?: ""
    fun setTargetApp(context: Context, packageName: String) =
        prefs(context).edit().putString("target_app_package", packageName).apply()

    // Rule 2: battery threshold
    const val DEFAULT_BATTERY_THRESHOLD = 20
    fun getBatteryThreshold(context: Context): Int = prefs(context).getInt("battery_threshold", DEFAULT_BATTERY_THRESHOLD)
    fun setBatteryThreshold(context: Context, percent: Int) =
        prefs(context).edit().putInt("battery_threshold", percent).apply()

    // Rule 2: which actions to take when battery drops
    fun getBatterySaverAutoSync(context: Context): Boolean = prefs(context).getBoolean("battery_saver_auto_sync", true)
    fun setBatterySaverAutoSync(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean("battery_saver_auto_sync", enabled).apply()

    fun getBatterySaverBrightness(context: Context): Boolean = prefs(context).getBoolean("battery_saver_brightness", true)
    fun setBatterySaverBrightness(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean("battery_saver_brightness", enabled).apply()

    // Rule 3: target Bluetooth device (empty = any device)
    fun getBluetoothTargetAddress(context: Context): String = prefs(context).getString("bt_target_address", "") ?: ""
    fun setBluetoothTargetAddress(context: Context, address: String) =
        prefs(context).edit().putString("bt_target_address", address).apply()

    fun getBluetoothTargetName(context: Context): String = prefs(context).getString("bt_target_name", "") ?: ""
    fun setBluetoothTargetName(context: Context, name: String) =
        prefs(context).edit().putString("bt_target_name", name).apply()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
