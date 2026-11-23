package com.herchenmx.automationapp.triggers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.herchenmx.automationapp.engine.Trigger
import timber.log.Timber

/**
 * Fires when a Bluetooth audio device connects or disconnects.
 *
 * @param targetDeviceAddress MAC address of the specific device to watch, or empty for any device.
 */
class BluetoothAudioTrigger(
    override val id: String,
    private val context: Context,
    private val targetDeviceAddress: String = "",
) : Trigger {

    private var callback: ((Boolean) -> Unit)? = null
    private var currentlyActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val state = intent.getIntExtra(
                BluetoothAdapter.EXTRA_CONNECTION_STATE,
                BluetoothAdapter.STATE_DISCONNECTED,
            )
            // If a specific device is configured, ignore events from other devices.
            if (targetDeviceAddress.isNotBlank()) {
                val device: BluetoothDevice? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                } catch (_: Exception) { null }
                if (device?.address != targetDeviceAddress) return
            }
            val isConnected = state == BluetoothAdapter.STATE_CONNECTED
            if (isConnected != currentlyActive) {
                currentlyActive = isConnected
                Timber.d("BluetoothAudioTrigger[$id]: connected=$isConnected device=${targetDeviceAddress.ifBlank { "any" }}")
                callback?.invoke(isConnected)
            }
        }
    }

    override fun start(onStateChange: (Boolean) -> Unit) {
        callback = onStateChange
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED))

        // Check initial state. getConnectedDevices(A2DP) requires BLUETOOTH_CONNECT on API 31+.
        val initiallyConnected = try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val connected = manager?.getConnectedDevices(BluetoothProfile.A2DP) ?: emptyList()
            if (targetDeviceAddress.isBlank()) {
                connected.isNotEmpty()
            } else {
                connected.any { it.address == targetDeviceAddress }
            }
        } catch (e: SecurityException) {
            Timber.w("BluetoothAudioTrigger[$id]: BLUETOOTH_CONNECT not granted, assuming disconnected")
            false
        }
        currentlyActive = initiallyConnected
        Timber.d("BluetoothAudioTrigger[$id]: started, initial=$initiallyConnected target=${targetDeviceAddress.ifBlank { "any" }}")
        if (initiallyConnected) onStateChange(true)
    }

    override fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        callback = null
        Timber.d("BluetoothAudioTrigger[$id]: stopped")
    }
}
