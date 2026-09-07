package com.example.sonata.ui

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sonata.data.AccountManager
import com.example.sonata.data.DiscordAccount
import com.example.sonata.service.RpcForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
private data class DiscordMeResponse(
    val id: String,
    val username: String,
    val avatar: String? = null
)

class AccountManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val accountManager = AccountManager(application)
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    var accounts by mutableStateOf(accountManager.getAccounts())
        private set

    fun addAccount(token: String) {
        viewModelScope.launch {
            val profile = fetchProfile(token)
            if (profile != null) {
                val newAccount = DiscordAccount(
                    token = token,
                    name = profile.username,
                    username = profile.username,
                    avatarUrl = profile.avatar?.let { "https://cdn.discordapp.com/avatars/${profile.id}/$it.png" }
                )
                
                val currentAccounts = accountManager.getAccounts()
                if (currentAccounts.none { it.token == token }) {
                    val updatedList = currentAccounts + newAccount
                    accountManager.saveAccounts(updatedList)
                    accounts = updatedList
                    
                    val intent = Intent(getApplication(), RpcForegroundService::class.java).apply {
                        putExtra("action", "CONNECT_ACCOUNT")
                        putExtra("token", token)
                    }
                    getApplication<Application>().startForegroundService(intent)
                }
            }
        }
    }

    fun removeAccount(account: DiscordAccount) {
        val updatedList = accounts.filter { it.token != account.token }
        accountManager.saveAccounts(updatedList)
        accounts = updatedList
        
        val intent = Intent(getApplication(), RpcForegroundService::class.java).apply {
            putExtra("action", "DISCONNECT_ACCOUNT")
            putExtra("token", account.token)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun toggleAccount(account: DiscordAccount, isEnabled: Boolean) {
        val updatedList = accounts.map {
            if (it.token == account.token) it.copy(isEnabled = isEnabled) else it
        }
        accountManager.saveAccounts(updatedList)
        accounts = updatedList
        
        val action = if (isEnabled) "CONNECT_ACCOUNT" else "DISCONNECT_ACCOUNT"
        val intent = Intent(getApplication(), RpcForegroundService::class.java).apply {
            putExtra("action", action)
            putExtra("token", account.token)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    private suspend fun fetchProfile(token: String): DiscordMeResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://discord.com/api/v10/users/@me")
            .header("Authorization", token)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    json.decodeFromString<DiscordMeResponse>(body)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
