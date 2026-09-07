package com.example.sonata.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DiscordAccount(
    val token: String,
    val name: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isEnabled: Boolean = true
)

class AccountManager(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "discord_accounts",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccounts(accounts: List<DiscordAccount>) {
        val json = Json.encodeToString(accounts)
        sharedPreferences.edit().putString("accounts", json).apply()
    }

    fun getAccounts(): List<DiscordAccount> {
        val json = sharedPreferences.getString("accounts", null) ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
