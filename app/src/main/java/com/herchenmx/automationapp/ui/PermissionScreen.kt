package com.herchenmx.automationapp.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.herchenmx.automationapp.util.PermissionChecker

private data class PermissionItem(
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val openSettings: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val permissions = remember(refreshKey) {
        listOf(
            PermissionItem(
                name = "Accessibility Service",
                description = "Detects foreground app changes in real time. Enable: Settings → Accessibility → Automation App.",
                isGranted = PermissionChecker.isAccessibilityEnabled(context),
                openSettings = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            ),
            PermissionItem(
                name = "Notification Listener",
                description = "Detects YT Music playback. Enable: Settings → Notifications → Notification access.",
                isGranted = PermissionChecker.isNotificationListenerEnabled(context),
                openSettings = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            ),
            PermissionItem(
                name = "Write Settings",
                description = "Controls brightness mode. Enable: Settings → Apps → Special app access → Modify system settings.",
                isGranted = Settings.System.canWrite(context),
                openSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    )
                },
            ),
            PermissionItem(
                name = "Do Not Disturb Access",
                description = "Controls DND mode. Enable: Settings → Apps → Special app access → Do Not Disturb.",
                isGranted = PermissionChecker.isDndAccessGranted(context),
                openSettings = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
            ),
            PermissionItem(
                name = "Battery Optimization Exempt",
                description = "Keeps service running in background. Enable: Settings → Apps → Automation App → Battery → Unrestricted.",
                isGranted = PermissionChecker.isBatteryOptimizationIgnored(context),
                openSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                    )
                },
            ),
        )
    }

    LaunchedEffect(refreshKey) {
        if (permissions.all { it.isGranted }) onAllGranted()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Setup Permissions") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "All permissions below are required before automation can run.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(permissions) { item -> PermissionCard(item) }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { refreshKey++ },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Refresh Status") }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall)
                Text(item.description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            if (item.isGranted) {
                Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            } else {
                Button(onClick = item.openSettings) { Text("Grant") }
            }
        }
    }
}
