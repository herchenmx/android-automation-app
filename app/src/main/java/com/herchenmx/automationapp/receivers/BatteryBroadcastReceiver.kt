package com.herchenmx.automationapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

// Static manifest receiver; battery monitoring is done dynamically in BatteryLevelTrigger.
class BatteryBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("BatteryBroadcastReceiver: action=${intent.action}")
    }
}
