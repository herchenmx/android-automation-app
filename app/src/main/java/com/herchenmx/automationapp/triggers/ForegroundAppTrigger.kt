package com.herchenmx.automationapp.triggers

import com.herchenmx.automationapp.engine.Trigger
import timber.log.Timber

/**
 * Fires when [targetPackage] enters the foreground.
 *
 * Uses AccessibilityService (TYPE_WINDOW_STATE_CHANGED) rather than UsageStatsManager
 * because AccessibilityService delivers real-time events with no polling loop, giving
 * sub-second detection latency needed for muting on app launch.
 */
class ForegroundAppTrigger(
    override val id: String,
    private val targetPackage: String,
) : Trigger {

    private var callback: ((Boolean) -> Unit)? = null
    private var currentlyActive = false

    override fun start(onStateChange: (Boolean) -> Unit) {
        callback = onStateChange
        ActiveListeners.register(targetPackage, this)
        Timber.d("ForegroundAppTrigger[$id]: watching $targetPackage")
    }

    override fun stop() {
        ActiveListeners.unregister(targetPackage, this)
        callback = null
        Timber.d("ForegroundAppTrigger[$id]: stopped")
    }

    internal fun onForegroundChanged(packageName: String) {
        val isActive = packageName == targetPackage
        if (isActive != currentlyActive) {
            currentlyActive = isActive
            Timber.d("ForegroundAppTrigger[$id]: package=$packageName active=$isActive")
            callback?.invoke(isActive)
        }
    }

    companion object ActiveListeners {
        private val listeners = mutableMapOf<String, MutableList<ForegroundAppTrigger>>()

        fun register(pkg: String, trigger: ForegroundAppTrigger) {
            listeners.getOrPut(pkg) { mutableListOf() }.add(trigger)
        }

        fun unregister(pkg: String, trigger: ForegroundAppTrigger) {
            listeners[pkg]?.remove(trigger)
        }

        fun notifyForegroundChanged(packageName: String) {
            listeners[packageName]?.forEach { it.onForegroundChanged(packageName) }
            listeners.entries
                .filter { it.key != packageName }
                .flatMap { it.value }
                .forEach { it.onForegroundChanged(packageName) }
        }
    }
}
