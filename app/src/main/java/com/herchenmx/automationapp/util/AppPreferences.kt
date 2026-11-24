package com.herchenmx.automationapp.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BtDevice(val address: String, val name: String)

object AppPreferences {
    private const val PREFS_NAME = "automation_config"

    // ── Rule 1: target app ────────────────────────────────────────────────────
    fun getTargetApp(context: Context): String = prefs(context).getString("target_app_package", "") ?: ""
    fun setTargetApp(context: Context, pkg: String) =
        prefs(context).edit().putString("target_app_package", pkg).apply()

    // ── Rule 2: battery threshold ─────────────────────────────────────────────
    const val DEFAULT_BATTERY_THRESHOLD = 20
    fun getBatteryThreshold(context: Context): Int = prefs(context).getInt("battery_threshold", DEFAULT_BATTERY_THRESHOLD)
    fun setBatteryThreshold(context: Context, percent: Int) =
        prefs(context).edit().putInt("battery_threshold", percent).apply()

    // ── Rule 2: which actions to take ─────────────────────────────────────────

    // Disable auto-sync
    fun getBatterySaverAutoSync(context: Context): Boolean = prefs(context).getBoolean("bs_auto_sync", true)
    fun setBatterySaverAutoSync(context: Context, v: Boolean) = prefs(context).edit().putBoolean("bs_auto_sync", v).apply()

    // Set brightness mode to manual
    fun getBatterySaverBrightnessMode(context: Context): Boolean = prefs(context).getBoolean("bs_brightness_mode", true)
    fun setBatterySaverBrightnessMode(context: Context, v: Boolean) = prefs(context).edit().putBoolean("bs_brightness_mode", v).apply()

    // Set brightness level (5–50 %)
    fun getBatterySaverBrightnessLevel(context: Context): Boolean = prefs(context).getBoolean("bs_brightness_level", false)
    fun setBatterySaverBrightnessLevel(context: Context, v: Boolean) = prefs(context).edit().putBoolean("bs_brightness_level", v).apply()
    fun getBatterySaverBrightnessLevelPct(context: Context): Int = prefs(context).getInt("bs_brightness_pct", 20)
    fun setBatterySaverBrightnessLevelPct(context: Context, pct: Int) = prefs(context).edit().putInt("bs_brightness_pct", pct).apply()

    // Shorten screen timeout (seconds: 30 / 60 / 120 / 300)
    fun getBatterySaverTimeout(context: Context): Boolean = prefs(context).getBoolean("bs_timeout", false)
    fun setBatterySaverTimeout(context: Context, v: Boolean) = prefs(context).edit().putBoolean("bs_timeout", v).apply()
    fun getBatterySaverTimeoutSecs(context: Context): Int = prefs(context).getInt("bs_timeout_secs", 60)
    fun setBatterySaverTimeoutSecs(context: Context, secs: Int) = prefs(context).edit().putInt("bs_timeout_secs", secs).apply()

    // Enable Do Not Disturb
    fun getBatterySaverDnd(context: Context): Boolean = prefs(context).getBoolean("bs_dnd", false)
    fun setBatterySaverDnd(context: Context, v: Boolean) = prefs(context).edit().putBoolean("bs_dnd", v).apply()

    // Lower media volume (0–50 %)
    fun getBatterySaverMediaVolume(context: Context): Boolean = prefs(context).getBoolean("bs_media_vol", false)
    fun setBatterySaverMediaVolume(context: Context, v: Boolean) = prefs(context).edit().putBoolean("bs_media_vol", v).apply()
    fun getBatterySaverMediaVolumePct(context: Context): Int = prefs(context).getInt("bs_media_vol_pct", 20)
    fun setBatterySaverMediaVolumePct(context: Context, pct: Int) = prefs(context).edit().putInt("bs_media_vol_pct", pct).apply()

    // ── Rule 3: media apps that trigger DND (default = YT Music) ─────────────
    private val DEFAULT_DND_APPS = listOf("com.google.android.apps.youtube.music")

    fun getDndTargetApps(context: Context): List<String> {
        val json = prefs(context).getString("dnd_target_apps", null)
            ?: return DEFAULT_DND_APPS
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { DEFAULT_DND_APPS }
    }

    fun setDndTargetApps(context: Context, packages: List<String>) {
        val arr = JSONArray()
        packages.forEach { arr.put(it) }
        prefs(context).edit().putString("dnd_target_apps", arr.toString()).apply()
    }

    // ── Rule 3: Bluetooth target devices (empty list = any device) ────────────
    fun getBluetoothTargetDevices(context: Context): List<BtDevice> {
        val json = prefs(context).getString("bt_target_devices", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                BtDevice(obj.getString("address"), obj.getString("name"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun setBluetoothTargetDevices(context: Context, devices: List<BtDevice>) {
        val arr = JSONArray()
        devices.forEach { arr.put(JSONObject().put("address", it.address).put("name", it.name)) }
        prefs(context).edit().putString("bt_target_devices", arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
