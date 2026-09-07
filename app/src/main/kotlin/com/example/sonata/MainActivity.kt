package com.example.sonata

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.sonata.data.AccountManager
import com.example.sonata.ui.MainScreen
import com.example.sonata.ui.OnboardingScreen
import com.example.sonata.util.PermissionHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (!PermissionHelper.hasNotificationAccess(this) || 
                        !PermissionHelper.isIgnoringBatteryOptimizations(this) ||
                        !PermissionHelper.hasPostNotificationPermission(this)) {
                        
                        OnboardingScreen(this) {
                            recreate()
                        }
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}
