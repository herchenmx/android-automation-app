package com.herchenmx.automationapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

// Static manifest receiver; Bluetooth monitoring is done dynamically in BluetoothAudioTrigger.
class BluetoothBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("BluetoothBroadcastReceiver: action=${intent.action}")
    }
}
