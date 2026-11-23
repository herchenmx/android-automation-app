package com.herchenmx.automationapp.triggers

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
 * @param targetDeviceAddresses MAC addresses to watch. Empty set = any device.
 */
class BluetoothAudioTrigger(
    override val id: String,
    private val context: Context,
    private val targetDeviceAddresses: Set<String> = emptySet(),
) : Trigger {

    private var callback: ((Boolean) -> Unit)? = null
    private var currentlyActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val state = intent.getIntExtra(
                android.bluetooth.BluetoothAdapter.EXTRA_CONNECTION_STATE,
                android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED,
            )
            if (targetDeviceAddresses.isNotEmpty()) {
                val device: BluetoothDevice? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                } catch (_: Exception) { null }
                if (device?.address !in targetDeviceAddresses) return
            }
            val isConnected = state == android.bluetooth.BluetoothAdapter.STATE_CONNECTED
            if (isConnected != currentlyActive) {
                currentlyActive = isConnected
                val target = if (targetDeviceAddresses.isEmpty()) "any" else targetDeviceAddresses.joinToString()
                Timber.d("BluetoothAudioTrigger[$id]: connected=$isConnected target=$target")
                callback?.invoke(isConnected)
            }
        }
    }

    override fun start(onStateChange: (Boolean) -> Unit) {
        callback = onStateChange
        context.registerReceiver(
            receiver,
            IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED),
        )
        val initiallyConnected = try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val connected = manager?.getConnectedDevices(BluetoothProfile.A2DP) ?: emptyList()
            if (targetDeviceAddresses.isEmpty()) {
                connected.isNotEmpty()
            } else {
                connected.any { it.address in targetDeviceAddresses }
            }
        } catch (e: SecurityException) {
            Timber.w("BluetoothAudioTrigger[$id]: BLUETOOTH_CONNECT not granted, assuming disconnected")
            false
        }
        currentlyActive = initiallyConnected
        val target = if (targetDeviceAddresses.isEmpty()) "any" else targetDeviceAddresses.joinToString()
        Timber.d("BluetoothAudioTrigger[$id]: started initial=$initiallyConnected target=$target")
        if (initiallyConnected) onStateChange(true)
    }

    override fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        callback = null
        Timber.d("BluetoothAudioTrigger[$id]: stopped")
    }
}
