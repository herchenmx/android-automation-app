package com.herchenmx.automationapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.herchenmx.automationapp.ui.PermissionScreen
import com.herchenmx.automationapp.ui.RuleListScreen
import com.herchenmx.automationapp.ui.theme.AutomationAppTheme
import com.herchenmx.automationapp.util.PermissionChecker

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled via refresh on PermissionScreen */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            AutomationAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    // rememberSaveable survives Activity recreation (config changes, brief backgrounding).
    var allPermissionsGranted by rememberSaveable { mutableStateOf(false) }
    if (!allPermissionsGranted) {
        PermissionScreen(onAllGranted = { allPermissionsGranted = true })
    } else {
        RuleListScreen()
    }
}
