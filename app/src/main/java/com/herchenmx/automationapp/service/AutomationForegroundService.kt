package com.herchenmx.automationapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.herchenmx.automationapp.MainActivity
import com.herchenmx.automationapp.R
import com.herchenmx.automationapp.engine.RuleEngine
import com.herchenmx.automationapp.engine.StateManager
import com.herchenmx.automationapp.rules.HardcodedRules
import com.herchenmx.automationapp.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

class AutomationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var ruleEngine: RuleEngine
    private lateinit var stateManager: StateManager
    private lateinit var notificationManager: NotificationManager

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != ACTION_TOGGLE_RULE) return
            val ruleId = intent.getStringExtra(EXTRA_RULE_ID) ?: return
            val newEnabled = intent.getBooleanExtra(EXTRA_RULE_ENABLED, true)
            AppPreferences.setRuleEnabled(ctx, ruleId, newEnabled)
            ruleEngine.setRuleEnabled(ruleId, newEnabled)
            updateNotification()
            Timber.d("AutomationForegroundService: toggled $ruleId -> $newEnabled")
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                getString(R.string.channel_id),
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        stateManager = StateManager()
        ruleEngine = RuleEngine(stateManager, serviceScope)
        val rules = HardcodedRules.buildRules(applicationContext, stateManager)
        rules.forEach { ruleEngine.addRule(it) }

        val filter = IntentFilter(ACTION_TOGGLE_RULE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, toggleReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(toggleReceiver, filter)
        }
        Timber.d("AutomationForegroundService: created with ${rules.size} rules")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        ruleEngine.startAll()
        Timber.d("AutomationForegroundService: started")
        return START_STICKY
    }

    override fun onDestroy() {
        ruleEngine.stopAll()
        serviceScope.cancel()
        try { unregisterReceiver(toggleReceiver) } catch (_: Exception) {}
        Timber.d("AutomationForegroundService: destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification() = run {
        val channelId = getString(R.string.channel_id)
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )

        NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(buildStatusText())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(ruleToggleAction(RULE_ID_FOREGROUND, "App Audio", 1))
            .addAction(ruleToggleAction(RULE_ID_BATTERY,    "Battery",   2))
            .addAction(ruleToggleAction(RULE_ID_DND,        "BT DND",    3))
            .build()
    }

    private fun buildStatusText(): String {
        val parts = listOf(
            RULE_ID_FOREGROUND to "App",
            RULE_ID_BATTERY    to "Battery",
            RULE_ID_DND        to "DND",
        ).map { (id, label) ->
            val on = AppPreferences.getRuleEnabled(this, id)
            "$label ${if (on) "●" else "○"}"
        }
        return parts.joinToString("  ")
    }

    private fun ruleToggleAction(ruleId: String, label: String, requestCode: Int): NotificationCompat.Action {
        val enabled = AppPreferences.getRuleEnabled(this, ruleId)
        val newEnabled = !enabled
        val intent = Intent(ACTION_TOGGLE_RULE).apply {
            putExtra(EXTRA_RULE_ID, ruleId)
            putExtra(EXTRA_RULE_ENABLED, newEnabled)
            setPackage(packageName)
        }
        val pi = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = android.R.drawable.checkbox_on_background
        val displayLabel = "$label ${if (enabled) "●" else "○"}"
        return NotificationCompat.Action(icon, displayLabel, pi)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE_RULE  = "com.herchenmx.automationapp.TOGGLE_RULE"
        const val EXTRA_RULE_ID       = "rule_id"
        const val EXTRA_RULE_ENABLED  = "rule_enabled"
        const val RULE_ID_FOREGROUND  = "foreground_app_volume"
        const val RULE_ID_BATTERY     = "battery_saver"
        const val RULE_ID_DND         = "yt_music_bt_dnd"
    }
}
