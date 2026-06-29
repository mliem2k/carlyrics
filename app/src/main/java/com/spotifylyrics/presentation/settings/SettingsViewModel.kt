package com.spotifylyrics.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifylyrics.domain.usecase.ClearCacheUseCase
import com.spotifylyrics.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val clearCacheUseCase: ClearCacheUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.CONFLATED)
    val messages = _messages.receiveAsFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.isLrclibEnabled(),
                settingsRepository.isGeniusEnabled(),
                settingsRepository.isMusixmatchEnabled(),
                settingsRepository.isAutoFetchEnabled()
            ) { lrclib, genius, musixmatch, autoFetch ->
                SettingsUiState(
                    lrclibEnabled = lrclib,
                    geniusEnabled = genius,
                    musixmatchEnabled = musixmatch,
                    autoFetchEnabled = autoFetch
                )
            }.collect { _uiState.value = it }
        }
    }

    fun toggleLrclib(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLrclibEnabled(enabled) }
    }

    fun toggleGenius(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGeniusEnabled(enabled) }
    }

    fun toggleMusixmatch(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMusixmatchEnabled(enabled) }
    }

    fun toggleAutoFetch(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoFetch(enabled) }
    }

    fun clearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
            _messages.send("Cache cleared")
        }
    }
}

data class SettingsUiState(
    val lrclibEnabled: Boolean = true,
    val geniusEnabled: Boolean = true,
    val musixmatchEnabled: Boolean = true,
    val autoFetchEnabled: Boolean = true
)
