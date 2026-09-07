package com.example.sonata.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sonata.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RpcCustomizationViewModel(private val repository: DataStoreRepository) : ViewModel() {
    private val _config = MutableStateFlow(RpcCustomizationConfig())
    val config = _config.asStateFlow()

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
}
