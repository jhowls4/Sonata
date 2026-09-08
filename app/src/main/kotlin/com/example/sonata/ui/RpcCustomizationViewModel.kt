package com.example.sonata.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sonata.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RpcCustomizationViewModel(private val repository: DataStoreRepository) : ViewModel() {
    private val _config = MutableStateFlow(RpcCustomizationConfig())
    val config = _config.asStateFlow()

    val presets = repository.getPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getGlobalConfig().collect {
                _config.value = it
            }
        }
    }

    fun updateConfig(newConfig: RpcCustomizationConfig) {
        _config.value = newConfig
        viewModelScope.launch {
            repository.saveGlobalConfig(newConfig)
        }
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            val currentPresets = presets.value
            val updated = currentPresets.filterNot { it.name == name } + RpcPreset(name, config.value)
            repository.savePresets(updated)
        }
    }

    fun deletePreset(name: String) {
        viewModelScope.launch {
            val updated = presets.value.filterNot { it.name == name }
            repository.savePresets(updated)
        }
    }

    fun applyPreset(preset: RpcPreset) {
        updateConfig(preset.config)
    }
}
