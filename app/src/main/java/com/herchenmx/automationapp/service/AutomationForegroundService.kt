package com.herchenmx.automationapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.herchenmx.automationapp.MainActivity
import com.herchenmx.automationapp.R
import com.herchenmx.automationapp.engine.RuleEngine
import com.herchenmx.automationapp.engine.StateManager
import com.herchenmx.automationapp.rules.HardcodedRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

class AutomationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var ruleEngine: RuleEngine
    private lateinit var stateManager: StateManager

    override fun onCreate() {
        super.onCreate()
        stateManager = StateManager()
        ruleEngine = RuleEngine(stateManager, serviceScope)
        val rules = HardcodedRules.buildRules(applicationContext, stateManager)
        rules.forEach { ruleEngine.addRule(it) }
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
        Timber.d("AutomationForegroundService: destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() = run {
        val channelId = getString(R.string.channel_id)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
