package com.herchenmx.automationapp.util

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import com.herchenmx.automationapp.service.AutomationAccessibilityService
import com.herchenmx.automationapp.service.AutomationNotificationListenerService

object PermissionChecker {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AutomationAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(expected)
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AutomationNotificationListenerService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(expected)
    }

    fun isDndAccessGranted(context: Context): Boolean =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .isNotificationPolicyAccessGranted

    fun isBatteryOptimizationIgnored(context: Context): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)
}
