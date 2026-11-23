package com.herchenmx.automationapp.ui

import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.herchenmx.automationapp.service.AutomationForegroundService
import com.herchenmx.automationapp.util.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleListScreen() {
    val context = LocalContext.current
    var serviceRunning by remember { mutableStateOf(false) }

    var rule1Enabled by remember { mutableStateOf(true) }
    var rule1Expanded by remember { mutableStateOf(false) }
    var targetApp by remember { mutableStateOf(AppPreferences.getTargetApp(context)) }
    var showAppPicker by remember { mutableStateOf(false) }

    var rule2Enabled by remember { mutableStateOf(true) }
    var rule2Expanded by remember { mutableStateOf(false) }
    var batteryThreshold by remember { mutableFloatStateOf(AppPreferences.getBatteryThreshold(context).toFloat()) }

    var rule3Enabled by remember { mutableStateOf(true) }
    var rule3Expanded by remember { mutableStateOf(false) }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onSelect = { pkg ->
                targetApp = pkg
                AppPreferences.setTargetApp(context, pkg)
            },
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
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
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
                Spacer(Modifier.height(8.dp))
            }

            item {
                RuleCard(
                    name = "Mute on Foreground App",
                    description = "Mutes speaker when target app is in foreground.",
                    isEnabled = rule1Enabled,
                    isExpanded = rule1Expanded,
                    onToggle = { rule1Enabled = it },
                    onExpandToggle = { rule1Expanded = !rule1Expanded },
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        Text("Target app package", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetApp,
                            onValueChange = {
                                targetApp = it
                                AppPreferences.setTargetApp(context, it)
                            },
                            placeholder = { Text("com.example.app") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                TextButton(onClick = { showAppPicker = true }) {
                                    Text("Browse")
                                }
                            },
                        )
                        if (targetApp.isBlank()) {
                            Text(
                                "No app set — rule will not trigger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            item {
                RuleCard(
                    name = "Battery Saver",
                    description = "Disables auto-sync and dims brightness below threshold.",
                    isEnabled = rule2Enabled,
                    isExpanded = rule2Expanded,
                    onToggle = { rule2Enabled = it },
                    onExpandToggle = { rule2Expanded = !rule2Expanded },
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        Text(
                            "Battery threshold: ${batteryThreshold.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Slider(
                            value = batteryThreshold,
                            onValueChange = { batteryThreshold = it },
                            onValueChangeFinished = {
                                AppPreferences.setBatteryThreshold(context, batteryThreshold.roundToInt())
                            },
                            valueRange = 5f..80f,
                            steps = 14,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Rule activates when battery drops below this level.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                RuleCard(
                    name = "DND for YT Music + Bluetooth",
                    description = "Enables DND when YT Music plays over Bluetooth audio.",
                    isEnabled = rule3Enabled,
                    isExpanded = rule3Expanded,
                    onToggle = { rule3Enabled = it },
                    onExpandToggle = { rule3Expanded = !rule3Expanded },
                ) {
                    Text(
                        "Triggers when YouTube Music is playing and a Bluetooth audio device is connected. No additional configuration needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    name: String,
    description: String,
    isEnabled: Boolean,
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onExpandToggle: () -> Unit,
    configContent: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Configure",
                    )
                }
                Switch(checked = isEnabled, onCheckedChange = onToggle)
            }
            if (isExpanded) {
                HorizontalDivider()
                configContent()
            }
        }
    }
}

private data class AppInfo(val name: String, val packageName: String)

@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            @Suppress("DEPRECATION")
            val resolved: List<ResolveInfo> = pm.queryIntentActivities(launchIntent, 0)
            apps = resolved
                .distinctBy { it.activityInfo.packageName }
                .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
                .sortedBy { it.name }
            loading = false
        }
    }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter {
            it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(app.packageName); onDismiss() }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(app.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
