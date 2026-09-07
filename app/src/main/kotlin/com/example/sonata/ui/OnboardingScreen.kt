package com.example.sonata.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sonata.util.PermissionHelper

@Composable
fun OnboardingScreen(context: android.content.Context, onFinished: () -> Unit) {
    // State for permissions using checks from PermissionHelper
    var hasNotifAccess by remember { mutableStateOf(PermissionHelper.hasNotificationAccess(context)) }
    var hasPostNotif by remember { mutableStateOf(PermissionHelper.hasPostNotificationPermission(context)) }
    var isIgnoringBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }

    // Launcher for standard runtime permissions (e.g., POST_NOTIFICATIONS)
    val postNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotif = isGranted
    }

    // Lifecycle observer to re-verify state when returning to the app from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Verify if permissions were granted while the user was away
                hasNotifAccess = PermissionHelper.hasNotificationAccess(context)
                isIgnoringBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
                hasPostNotif = PermissionHelper.hasPostNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Sonata",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We need a few permissions to function correctly in the background.",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Notification Listener Access (Special System Permission)
        PermissionRow(
            title = "Notification Access",
            isGranted = hasNotifAccess,
            onGrantClick = { PermissionHelper.openNotificationListenerSettings(context) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // 2. Post Notifications (Standard Runtime Permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                title = "Post Notifications",
                isGranted = hasPostNotif,
                onGrantClick = { postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Battery Optimization Bypass (Special System Permission)
        PermissionRow(
            title = "Ignore Battery Optimizations",
            isGranted = isIgnoringBattery,
            onGrantClick = { PermissionHelper.requestIgnoreBatteryOptimizations(context) }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onFinished,
            enabled = hasNotifAccess && isIgnoringBattery && hasPostNotif,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            onClick = onGrantClick,
            enabled = !isGranted
        ) {
            Text(if (isGranted) "Granted" else "Grant")
        }
    }
}
