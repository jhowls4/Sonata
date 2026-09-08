package com.example.sonata.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sonata.data.DiscordAccount
import com.example.sonata.ui.DiscordLoginWebView
import com.example.sonata.ui.AccountManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToCustomization: () -> Unit) {
    val viewModel: AccountManagerViewModel = viewModel()
    val accounts = viewModel.accounts
    
    var showManualDialog by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var newToken by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discord Accounts") },
                actions = {
                    // Option A: Top App Bar Action Button
                    IconButton(onClick = onNavigateToCustomization) {
                        Icon(Icons.Default.Settings, contentDescription = "RPC Customization")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Option B: Extra Floating Action Button
                SmallFloatingActionButton(
                    onClick = onNavigateToCustomization,
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Customize RPC")
                }

                FloatingActionButton(
                    onClick = { showWebView = true },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("Login", modifier = Modifier.padding(horizontal = 16.dp))
                }
                FloatingActionButton(onClick = { showManualDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Manually")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Text("Discord Accounts", style = MaterialTheme.typography.headlineMedium) // Removed as it's now in TopAppBar
            // Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(accounts) { account ->
                    AccountItem(
                        account = account,
                        onDelete = {
                            viewModel.removeAccount(account)
                        },
                        onToggle = { isEnabled ->
                            viewModel.toggleAccount(account, isEnabled)
                        }
                    )
                }
            }
        }
    }

    if (showWebView) {
        Dialog(
            onDismissRequest = { showWebView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DiscordLoginWebView(
                onTokenExtracted = { token ->
                    viewModel.addAccount(token)
                    showWebView = false
                },
                onDismiss = { showWebView = false }
            )
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Add Discord Account") },
            text = {
                Column {
                    TextField(value = newName, onValueChange = { newName = it }, label = { Text("Account Name") })
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(value = newToken, onValueChange = { newToken = it }, label = { Text("Token") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newToken.isNotBlank() && newName.isNotBlank()) {
                        viewModel.addAccount(newToken) // Using ViewModel to handle adding
                        showManualDialog = false
                        newToken = ""
                        newName = ""
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AccountItem(account: DiscordAccount, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model = account.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 12.dp),
                error = painterResource(android.R.drawable.ic_menu_report_image) // Fallback
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (account.username != null) "@${account.username}" else "Token: ${account.token.take(10)}...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = account.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
