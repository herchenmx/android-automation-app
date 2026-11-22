package com.herchenmx.automationapp.triggers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.herchenmx.automationapp.engine.Trigger
import timber.log.Timber

/**
 * Fires when any Bluetooth audio device is connected.
 * Checks initial state at start so rules activate immediately if already connected.
 */
class BluetoothAudioTrigger(
    override val id: String,
    private val context: Context,
) : Trigger {

    private var callback: ((Boolean) -> Unit)? = null
    private var currentlyActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val state = intent.getIntExtra(
                BluetoothAdapter.EXTRA_CONNECTION_STATE,
                BluetoothAdapter.STATE_DISCONNECTED
            )
            val isConnected = state == BluetoothAdapter.STATE_CONNECTED
            if (isConnected != currentlyActive) {
                currentlyActive = isConnected
                Timber.d("BluetoothAudioTrigger[$id]: connected=$isConnected")
                callback?.invoke(isConnected)
            }
        }
    }

    override fun start(onStateChange: (Boolean) -> Unit) {
        callback = onStateChange
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED))
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val initiallyConnected = adapter?.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
        currentlyActive = initiallyConnected
        Timber.d("BluetoothAudioTrigger[$id]: started, initial connected=$initiallyConnected")
        if (initiallyConnected) onStateChange(true)
    }

    override fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        callback = null
        Timber.d("BluetoothAudioTrigger[$id]: stopped")
    }
}
