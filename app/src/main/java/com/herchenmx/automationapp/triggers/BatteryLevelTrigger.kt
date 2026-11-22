package com.herchenmx.automationapp.triggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.herchenmx.automationapp.engine.Trigger
import timber.log.Timber

/**
 * Fires when battery drops below [thresholdPercent]; clears when it rises above.
 * Uses ACTION_BATTERY_CHANGED which is a sticky broadcast — no manifest registration needed.
 */
class BatteryLevelTrigger(
    override val id: String,
    private val context: Context,
    private val thresholdPercent: Int,
) : Trigger {

    private var callback: ((Boolean) -> Unit)? = null
    private var currentlyActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level < 0) return
            val percent = level * 100 / scale
            val shouldBeActive = percent < thresholdPercent
            if (shouldBeActive != currentlyActive) {
                currentlyActive = shouldBeActive
                Timber.d("BatteryLevelTrigger[$id]: battery=${percent}% threshold=${thresholdPercent}% active=$shouldBeActive")
                callback?.invoke(shouldBeActive)
            }
        }
    }

    override fun start(onStateChange: (Boolean) -> Unit) {
        callback = onStateChange
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        Timber.d("BatteryLevelTrigger[$id]: started, threshold=${thresholdPercent}%")
    }

    override fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        callback = null
        Timber.d("BatteryLevelTrigger[$id]: stopped")
    }
}
