package com.herchenmx.automationapp.actions

import android.app.NotificationManager
import android.content.Context
import com.herchenmx.automationapp.engine.Action
import com.herchenmx.automationapp.engine.StateManager
import timber.log.Timber

class DndModeAction(
    override val id: String,
    private val context: Context,
    private val stateManager: StateManager,
    private val dndMode: Int = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
) : Action {

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override suspend fun execute() {
        try {
            val current = notificationManager.currentInterruptionFilter
            stateManager.save(id, current)
            notificationManager.setInterruptionFilter(dndMode)
            Timber.d("DndModeAction[$id]: saved DND=$current set=$dndMode")
        } catch (e: Exception) {
            Timber.e(e, "DndModeAction[$id]: failed to set DND mode (permission granted?)")
        }
    }

    override suspend fun revert() {
        val saved = stateManager.restore<Int>(id)
        if (saved != null) {
            try {
                notificationManager.setInterruptionFilter(saved)
                Timber.d("DndModeAction[$id]: restored DND=$saved")
            } catch (e: Exception) {
                Timber.e(e, "DndModeAction[$id]: failed to restore DND mode")
            }
        } else {
            Timber.w("DndModeAction[$id]: no saved DND mode to restore")
        }
    }
}
