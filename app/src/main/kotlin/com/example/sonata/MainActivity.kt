package com.example.sonata

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sonata.data.DataStoreRepository
import com.example.sonata.ui.MainScreen
import com.example.sonata.ui.OnboardingScreen
import com.example.sonata.ui.RpcCustomizationScreen
import com.example.sonata.ui.RpcCustomizationViewModel
import com.example.sonata.util.PermissionHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SonataApp()
                }
            }
        }
    }
}

@Composable
fun SonataApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    if (!PermissionHelper.hasNotificationAccess(context) ||
        !PermissionHelper.isIgnoringBatteryOptimizations(context) ||
        !PermissionHelper.hasPostNotificationPermission(context)
    ) {
        OnboardingScreen(context) {
            // Re-run the activity to refresh permission checks
            (context as? ComponentActivity)?.recreate()
        }
    } else {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onNavigateToCustomization = {
                        navController.navigate("rpc_customization")
                    }
                )
            }
            composable("rpc_customization") {
                val repository = DataStoreRepository(context)
                val customizationViewModel: RpcCustomizationViewModel = viewModel {
                    RpcCustomizationViewModel(repository)
                }
                RpcCustomizationScreen(
                    viewModel = customizationViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
