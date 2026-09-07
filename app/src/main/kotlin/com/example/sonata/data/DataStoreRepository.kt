package com.example.sonata.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rpc_settings")

class DataStoreRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val GLOBAL_CONFIG_KEY = stringPreferencesKey("global_rpc_config")
        private fun accountConfigKey(token: String) = stringPreferencesKey("rpc_config_$token")
    }

    fun getGlobalConfig(): Flow<RpcCustomizationConfig> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[GLOBAL_CONFIG_KEY]
            if (jsonString != null) {
                try {
                    json.decodeFromString(jsonString)
                } catch (e: Exception) {
                    RpcCustomizationConfig()
                }
            } else {
                RpcCustomizationConfig()
            }
        }
    }

    suspend fun saveGlobalConfig(config: RpcCustomizationConfig) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_CONFIG_KEY] = json.encodeToString(config)
        }
    }

    fun getAccountConfig(token: String): Flow<RpcCustomizationConfig?> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[accountConfigKey(token)]
            jsonString?.let {
                try {
                    json.decodeFromString<RpcCustomizationConfig>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun saveAccountConfig(token: String, config: RpcCustomizationConfig?) {
        context.dataStore.edit { preferences ->
            val key = accountConfigKey(token)
            if (config != null) {
                preferences[key] = json.encodeToString(config)
            } else {
                preferences.remove(key)
            }
        }
    }
}
