package com.herchenmx.automationapp.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.herchenmx.automationapp.service.AutomationForegroundService
import com.herchenmx.automationapp.service.AutomationForegroundService.Companion.ACTION_TOGGLE_RULE
import com.herchenmx.automationapp.service.AutomationForegroundService.Companion.EXTRA_RULE_ENABLED
import com.herchenmx.automationapp.service.AutomationForegroundService.Companion.EXTRA_RULE_ID
import com.herchenmx.automationapp.util.AppPreferences
import com.herchenmx.automationapp.util.BtDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serviceRunning by remember { mutableStateOf(false) }
    var restarting by remember { mutableStateOf(false) }

    // ── Rule 1 ────────────────────────────────────────────────────────────────
    var rule1Enabled by remember { mutableStateOf(AppPreferences.getRuleEnabled(context, "foreground_app_volume")) }
    var rule1Expanded by remember { mutableStateOf(false) }
    var targetApp by remember { mutableStateOf(AppPreferences.getTargetApp(context)) }
    var showAppPicker by remember { mutableStateOf(false) }

    // ── Rule 2 ────────────────────────────────────────────────────────────────
    var rule2Enabled by remember { mutableStateOf(AppPreferences.getRuleEnabled(context, "battery_saver")) }
    var rule2Expanded by remember { mutableStateOf(false) }
    var batteryThreshold by remember { mutableFloatStateOf(AppPreferences.getBatteryThreshold(context).toFloat()) }

    var bsAutoSync by remember { mutableStateOf(AppPreferences.getBatterySaverAutoSync(context)) }
    var bsBrightnessMode by remember { mutableStateOf(AppPreferences.getBatterySaverBrightnessMode(context)) }
    var bsBrightnessLevel by remember { mutableStateOf(AppPreferences.getBatterySaverBrightnessLevel(context)) }
    var bsBrightnessLevelPct by remember { mutableFloatStateOf(AppPreferences.getBatterySaverBrightnessLevelPct(context).toFloat()) }
    var bsTimeout by remember { mutableStateOf(AppPreferences.getBatterySaverTimeout(context)) }
    var bsTimeoutSecs by remember { mutableIntStateOf(AppPreferences.getBatterySaverTimeoutSecs(context)) }
    var bsDnd by remember { mutableStateOf(AppPreferences.getBatterySaverDnd(context)) }
    var bsMediaVolume by remember { mutableStateOf(AppPreferences.getBatterySaverMediaVolume(context)) }
    var bsMediaVolumePct by remember { mutableFloatStateOf(AppPreferences.getBatterySaverMediaVolumePct(context).toFloat()) }

    // ── Rule 3 ────────────────────────────────────────────────────────────────
    var rule3Enabled by remember { mutableStateOf(AppPreferences.getRuleEnabled(context, "yt_music_bt_dnd")) }
    var rule3Expanded by remember { mutableStateOf(false) }
    var btDevices by remember { mutableStateOf(AppPreferences.getBluetoothTargetDevices(context)) }
    var showBtPicker by remember { mutableStateOf(false) }
    var dndApps by remember { mutableStateOf(AppPreferences.getDndTargetApps(context)) }
    var showDndAppPicker by remember { mutableStateOf(false) }

    val btPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showBtPicker = true
    }

    // ── Config-change tracking ────────────────────────────────────────────────
    val currentConfig = listOf(
        targetApp, batteryThreshold, bsAutoSync, bsBrightnessMode, bsBrightnessLevel,
        bsBrightnessLevelPct, bsTimeout, bsTimeoutSecs, bsDnd, bsMediaVolume, bsMediaVolumePct,
        btDevices.joinToString { it.address },
        dndApps.joinToString(),
    ).joinToString("|")
    var appliedConfig by remember { mutableStateOf(currentConfig) }
    val configDirty = serviceRunning && currentConfig != appliedConfig

    fun startService() {
        context.startForegroundService(Intent(context, AutomationForegroundService::class.java))
        serviceRunning = true; appliedConfig = currentConfig
    }
    fun stopService() {
        context.stopService(Intent(context, AutomationForegroundService::class.java))
        serviceRunning = false
    }
    fun toggleRule(ruleId: String, enabled: Boolean) {
        AppPreferences.setRuleEnabled(context, ruleId, enabled)
        if (serviceRunning) {
            context.sendBroadcast(Intent(ACTION_TOGGLE_RULE).apply {
                putExtra(EXTRA_RULE_ID, ruleId)
                putExtra(EXTRA_RULE_ENABLED, enabled)
                setPackage(context.packageName)
            })
        }
    }

    fun restartService() {
        restarting = true
        scope.launch {
            context.stopService(Intent(context, AutomationForegroundService::class.java))
            delay(600L)
            context.startForegroundService(Intent(context, AutomationForegroundService::class.java))
            appliedConfig = currentConfig; restarting = false
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onSelect = { pkg -> targetApp = pkg; AppPreferences.setTargetApp(context, pkg) },
        )
    }
    if (showBtPicker) {
        BluetoothMultiPickerDialog(
            currentDevices = btDevices,
            onDismiss = { showBtPicker = false },
            onApply = { selected ->
                btDevices = selected
                AppPreferences.setBluetoothTargetDevices(context, selected)
            },
        )
    }
    if (showDndAppPicker) {
        AppMultiPickerDialog(
            currentPackages = dndApps,
            onDismiss = { showDndAppPicker = false },
            onApply = { selected ->
                dndApps = selected
                AppPreferences.setDndTargetApps(context, selected)
            },
        )
    }

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation Rules") },
                actions = {
                    val (statusText, statusColor) = when {
                        restarting    -> "↺ Restarting" to MaterialTheme.colorScheme.tertiary
                        configDirty   -> "● Restart needed" to MaterialTheme.colorScheme.error
                        serviceRunning -> "● Running" to MaterialTheme.colorScheme.primary
                        else          -> "○ Stopped" to MaterialTheme.colorScheme.error
                    }
                    Text(statusText, color = statusColor, modifier = Modifier.padding(end = 16.dp))
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Service controls
            item {
                if (!serviceRunning) {
                    Button(onClick = ::startService, modifier = Modifier.fillMaxWidth(), enabled = !restarting) {
                        Text("Start Service")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = ::stopService, modifier = Modifier.weight(1f), enabled = !restarting) {
                            Text("Stop")
                        }
                        Button(
                            onClick = ::restartService,
                            modifier = Modifier.weight(1f),
                            enabled = !restarting,
                            colors = if (configDirty) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                     else ButtonDefaults.buttonColors(),
                        ) {
                            Text(if (restarting) "Restarting…" else if (configDirty) "↺ Apply Changes" else "↺ Restart")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Rule 1 ────────────────────────────────────────────────────────
            item {
                RuleCard(
                    name = "Suppress App Audio on Foreground",
                    description = "Silences the target app's video/reel audio while it's in the foreground. Background music is unaffected.",
                    isEnabled = rule1Enabled, isExpanded = rule1Expanded,
                    onToggle = { rule1Enabled = it; toggleRule("foreground_app_volume", it) },
                    onExpandToggle = { rule1Expanded = !rule1Expanded },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                        Text("Target app package", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetApp,
                            onValueChange = { targetApp = it; AppPreferences.setTargetApp(context, it) },
                            placeholder = { Text("com.example.app") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = { TextButton(onClick = { showAppPicker = true }) { Text("Browse") } },
                        )
                        if (targetApp.isBlank()) {
                            Text("No app set — rule will not trigger.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // ── Rule 2 ────────────────────────────────────────────────────────
            item {
                RuleCard(
                    name = "Battery Saver",
                    description = "Applies selected actions when battery drops below threshold.",
                    isEnabled = rule2Enabled, isExpanded = rule2Expanded,
                    onToggle = { rule2Enabled = it; toggleRule("battery_saver", it) },
                    onExpandToggle = { rule2Expanded = !rule2Expanded },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                        // Threshold slider
                        Text("Trigger below: ${batteryThreshold.roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = batteryThreshold,
                            onValueChange = { batteryThreshold = it },
                            onValueChangeFinished = { AppPreferences.setBatteryThreshold(context, batteryThreshold.roundToInt()) },
                            valueRange = 5f..80f, steps = 14, modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("Actions when triggered:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))

                        // 1. Disable auto-sync
                        ActionCheckbox("Disable auto-sync", bsAutoSync) {
                            bsAutoSync = it; AppPreferences.setBatterySaverAutoSync(context, it)
                        }

                        // 2. Set brightness to manual
                        ActionCheckbox("Set screen brightness to manual", bsBrightnessMode) {
                            bsBrightnessMode = it; AppPreferences.setBatterySaverBrightnessMode(context, it)
                        }

                        // 3. Set brightness level
                        ActionCheckbox("Set screen brightness level", bsBrightnessLevel) {
                            bsBrightnessLevel = it; AppPreferences.setBatterySaverBrightnessLevel(context, it)
                        }
                        if (bsBrightnessLevel) {
                            SubSlider(
                                label = "${bsBrightnessLevelPct.roundToInt()}% brightness",
                                value = bsBrightnessLevelPct,
                                range = 5f..50f, steps = 8,
                                onValueChange = { bsBrightnessLevelPct = it },
                                onValueChangeFinished = { AppPreferences.setBatterySaverBrightnessLevelPct(context, bsBrightnessLevelPct.roundToInt()) },
                            )
                        }

                        // 4. Shorten screen timeout
                        ActionCheckbox("Shorten screen timeout", bsTimeout) {
                            bsTimeout = it; AppPreferences.setBatterySaverTimeout(context, it)
                        }
                        if (bsTimeout) {
                            TimeoutSelector(
                                selected = bsTimeoutSecs,
                                onSelect = { bsTimeoutSecs = it; AppPreferences.setBatterySaverTimeoutSecs(context, it) },
                            )
                        }

                        // 5. Enable DND
                        ActionCheckbox("Enable Do Not Disturb", bsDnd) {
                            bsDnd = it; AppPreferences.setBatterySaverDnd(context, it)
                        }

                        // 6. Lower media volume
                        ActionCheckbox("Lower media volume", bsMediaVolume) {
                            bsMediaVolume = it; AppPreferences.setBatterySaverMediaVolume(context, it)
                        }
                        if (bsMediaVolume) {
                            SubSlider(
                                label = "${bsMediaVolumePct.roundToInt()}% of max",
                                value = bsMediaVolumePct,
                                range = 0f..50f, steps = 9,
                                onValueChange = { bsMediaVolumePct = it },
                                onValueChangeFinished = { AppPreferences.setBatterySaverMediaVolumePct(context, bsMediaVolumePct.roundToInt()) },
                            )
                        }

                        val anyEnabled = bsAutoSync || bsBrightnessMode || bsBrightnessLevel ||
                                bsTimeout || bsDnd || bsMediaVolume
                        if (!anyEnabled) {
                            Spacer(Modifier.height(4.dp))
                            Text("No actions selected — rule will trigger but do nothing.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ── Rule 3 ────────────────────────────────────────────────────────
            item {
                RuleCard(
                    name = "DND on Media + Bluetooth",
                    description = "Enables DND when selected apps play over Bluetooth audio.",
                    isEnabled = rule3Enabled, isExpanded = rule3Expanded,
                    onToggle = { rule3Enabled = it; toggleRule("yt_music_bt_dnd", it) },
                    onExpandToggle = { rule3Expanded = !rule3Expanded },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                        // Media apps
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Media apps", style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = { showDndAppPicker = true }) { Text("Edit") }
                        }
                        Spacer(Modifier.height(2.dp))
                        if (dndApps.isEmpty()) {
                            Text("No apps selected — rule will not trigger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        } else {
                            dndApps.forEach { pkg ->
                                Text("• $pkg", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))

                        // Bluetooth devices
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bluetooth devices", style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) showBtPicker = true
                                else btPermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }) { Text("Edit") }
                        }
                        Spacer(Modifier.height(2.dp))
                        if (btDevices.isEmpty()) {
                            Text("Any audio device — triggers for all Bluetooth connections.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            btDevices.forEach { device ->
                                Text("• ${device.name.ifBlank { "Unknown" }} (${device.address})",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ActionCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 2.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SubSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 40.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished,
            valueRange = range, steps = steps, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TimeoutSelector(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(30 to "30s", 60 to "1 min", 120 to "2 min", 300 to "5 min")
    Row(
        modifier = Modifier.padding(start = 40.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (secs, label) ->
            val isSelected = selected == secs
            if (isSelected) {
                Button(onClick = {}, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                OutlinedButton(onClick = { onSelect(secs) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    name: String, description: String,
    isEnabled: Boolean, isExpanded: Boolean,
    onToggle: (Boolean) -> Unit, onExpandToggle: () -> Unit,
    configContent: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
            if (isExpanded) { HorizontalDivider(); configContent() }
        }
    }
}

// ── App Picker ────────────────────────────────────────────────────────────────

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
            @Suppress("DEPRECATION")
            val resolved: List<ResolveInfo> = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }, 0
            )
            apps = resolved.distinctBy { it.activityInfo.packageName }
                .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
                .sortedBy { it.name }
            loading = false
        }
    }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.name.contains(query, true) || it.packageName.contains(query, true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App") },
        text = {
            Column {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    placeholder = { Text("Search…") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(Modifier.height(300.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { onSelect(app.packageName); onDismiss() }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(app.name, style = MaterialTheme.typography.bodyMedium)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// ── App Multi-Select Picker ───────────────────────────────────────────────────

@Composable
private fun AppMultiPickerDialog(
    currentPackages: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedPackages by remember { mutableStateOf(currentPackages.toMutableSet()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val resolved: List<ResolveInfo> = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }, 0
            )
            apps = resolved.distinctBy { it.activityInfo.packageName }
                .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
                .sortedBy { it.name }
            loading = false
        }
    }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.name.contains(query, true) || it.packageName.contains(query, true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Media Apps") },
        text = {
            Column {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    placeholder = { Text("Search…") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(Modifier.height(320.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            val checked = app.packageName in selectedPackages
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        selectedPackages = selectedPackages.toMutableSet().apply {
                                            if (checked) remove(app.packageName) else add(app.packageName)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selectedPackages = selectedPackages.toMutableSet().apply {
                                            if (it) add(app.packageName) else remove(app.packageName)
                                        }
                                    },
                                )
                                Column {
                                    Text(app.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedPackages.toList()); onDismiss() }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Bluetooth Multi-Select Picker ─────────────────────────────────────────────

@Composable
private fun BluetoothMultiPickerDialog(
    currentDevices: List<BtDevice>,
    onDismiss: () -> Unit,
    onApply: (List<BtDevice>) -> Unit,
) {
    val context = LocalContext.current
    var pairedDevices by remember { mutableStateOf<List<BtDevice>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    // Start with the current selection
    var selectedAddresses by remember { mutableStateOf(currentDevices.map { it.address }.toMutableSet()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager
            try {
                val bonded: Set<BluetoothDevice> = manager?.adapter?.bondedDevices ?: emptySet()
                pairedDevices = bonded.map { BtDevice(it.name ?: "Unknown", it.address) }.sortedBy { it.name }
            } catch (_: SecurityException) { permissionDenied = true }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bluetooth Devices") },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                permissionDenied -> Text(
                    "Bluetooth Connect permission was denied. Grant it in Settings → Apps → Automation App → Permissions.",
                    style = MaterialTheme.typography.bodySmall,
                )
                else -> LazyColumn(Modifier.height(320.dp)) {
                    // "Any device" row — selecting it clears the specific selection
                    item {
                        val anySelected = selectedAddresses.isEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { selectedAddresses = mutableSetOf() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = anySelected, onCheckedChange = { if (it) selectedAddresses = mutableSetOf() })
                            Column {
                                Text("Any audio device", style = MaterialTheme.typography.bodyMedium)
                                Text("Trigger for all Bluetooth connections", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                    if (pairedDevices.isEmpty()) {
                        item {
                            Text("No paired Bluetooth devices found.", style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
                        }
                    } else {
                        items(pairedDevices, key = { it.address }) { device ->
                            val checked = device.address in selectedAddresses
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        selectedAddresses = selectedAddresses.toMutableSet().apply {
                                            if (checked) remove(device.address) else add(device.address)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selectedAddresses = selectedAddresses.toMutableSet().apply {
                                            if (it) add(device.address) else remove(device.address)
                                        }
                                    },
                                )
                                Column {
                                    Text(device.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(device.address, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val selected = pairedDevices.filter { it.address in selectedAddresses }
                onApply(selected)
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
