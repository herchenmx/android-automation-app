package com.herchenmx.automationapp.engine

/**
 * Monitors some system state and fires callbacks when it changes.
 * Implementations observe things like foreground app, battery level, BT connection.
 */
interface Trigger {
    val id: String
    fun start(onStateChange: (active: Boolean) -> Unit)
    fun stop()
}
