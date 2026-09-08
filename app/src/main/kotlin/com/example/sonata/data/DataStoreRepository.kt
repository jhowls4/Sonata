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
        private val PRESETS_KEY = stringPreferencesKey("rpc_presets")
        private fun accountConfigKey(token: String) = stringPreferencesKey("rpc_config_$token")
    }

    fun getGlobalConfig(): Flow<RpcCustomizationConfig> {
        return context.dataStore.data.map { preferences ->
            preferences[GLOBAL_CONFIG_KEY]?.let {
                try { json.decodeFromString<RpcCustomizationConfig>(it) } catch (e: Exception) { null }
            } ?: RpcCustomizationConfig()
        }
    }

    suspend fun saveGlobalConfig(config: RpcCustomizationConfig) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_CONFIG_KEY] = json.encodeToString(config)
        }
    }

    fun getAccountConfig(token: String): Flow<RpcCustomizationConfig?> {
        return context.dataStore.data.map { preferences ->
            preferences[accountConfigKey(token)]?.let {
                try { json.decodeFromString<RpcCustomizationConfig>(it) } catch (e: Exception) { null }
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

    fun getPresets(): Flow<List<RpcPreset>> {
        return context.dataStore.data.map { preferences ->
            preferences[PRESETS_KEY]?.let {
                try { json.decodeFromString<List<RpcPreset>>(it) } catch (e: Exception) { emptyList() }
            } ?: emptyList()
        }
    }

    suspend fun savePresets(presets: List<RpcPreset>) {
        context.dataStore.edit { preferences ->
            preferences[PRESETS_KEY] = json.encodeToString(presets)
        }
    }
}
