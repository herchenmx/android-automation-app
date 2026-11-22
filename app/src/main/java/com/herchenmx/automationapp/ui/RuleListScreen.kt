package com.herchenmx.automationapp.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.herchenmx.automationapp.service.AutomationForegroundService

private data class RuleUiModel(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleListScreen() {
    val context = LocalContext.current
    var serviceRunning by remember { mutableStateOf(false) }
    var rules by remember {
        mutableStateOf(
            listOf(
                RuleUiModel(
                    id = "foreground_app_volume",
                    name = "Mute on Foreground App",
                    description = "Mutes speaker when target app is in foreground; restores volume when it leaves.",
                    isEnabled = true,
                ),
                RuleUiModel(
                    id = "battery_saver",
                    name = "Battery Saver",
                    description = "Disables auto-sync and manual brightness when battery < 50%.",
                    isEnabled = true,
                ),
                RuleUiModel(
                    id = "yt_music_bt_dnd",
                    name = "DND for YT Music + Bluetooth",
                    description = "Enables DND (priority only) when YT Music plays over a Bluetooth audio device.",
                    isEnabled = true,
                ),
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation Rules") },
                actions = {
                    Text(
                        text = if (serviceRunning) "● Running" else "○ Stopped",
                        color = if (serviceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Button(
                onClick = {
                    if (!serviceRunning) {
                        context.startForegroundService(Intent(context, AutomationForegroundService::class.java))
                    } else {
                        context.stopService(Intent(context, AutomationForegroundService::class.java))
                    }
                    serviceRunning = !serviceRunning
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (serviceRunning) "Stop Service" else "Start Service")
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(rule) { enabled ->
                        rules = rules.map { if (it.id == rule.id) it.copy(isEnabled = enabled) else it }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleCard(rule: RuleUiModel, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleSmall)
                Text(rule.description, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
        }
    }
}
