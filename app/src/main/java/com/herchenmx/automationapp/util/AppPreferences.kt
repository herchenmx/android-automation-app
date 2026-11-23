package com.herchenmx.automationapp.util

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "automation_config"
    private const val KEY_TARGET_APP = "target_app_package"
    private const val KEY_BATTERY_THRESHOLD = "battery_threshold"

    const val DEFAULT_BATTERY_THRESHOLD = 20

    fun getTargetApp(context: Context): String =
        prefs(context).getString(KEY_TARGET_APP, "") ?: ""

    fun setTargetApp(context: Context, packageName: String) =
        prefs(context).edit().putString(KEY_TARGET_APP, packageName).apply()

    fun getBatteryThreshold(context: Context): Int =
        prefs(context).getInt(KEY_BATTERY_THRESHOLD, DEFAULT_BATTERY_THRESHOLD)

    fun setBatteryThreshold(context: Context, percent: Int) =
        prefs(context).edit().putInt(KEY_BATTERY_THRESHOLD, percent).apply()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
