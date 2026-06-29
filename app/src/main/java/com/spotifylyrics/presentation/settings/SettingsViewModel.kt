package com.spotifylyrics.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifylyrics.domain.usecase.ClearCacheUseCase
import com.spotifylyrics.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SettingsScreen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val clearCacheUseCase: ClearCacheUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.isGeniusEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(geniusEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.isMusixmatchEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(musixmatchEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.isAutoFetchEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(autoFetchEnabled = enabled)
            }
        }
    }

    fun toggleGenius(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGeniusEnabled(enabled)
        }
    }

    fun toggleMusixmatch(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMusixmatchEnabled(enabled)
        }
    }

    fun toggleAutoFetch(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoFetch(enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
            _uiState.value = _uiState.value.copy(cacheCleared = true)
        }
    }

    fun cacheClearedHandled() {
        _uiState.value = _uiState.value.copy(cacheCleared = false)
    }
}

/**
 * UI state for SettingsScreen
 */
data class SettingsUiState(
    val geniusEnabled: Boolean = true,
    val musixmatchEnabled: Boolean = true,
    val autoFetchEnabled: Boolean = true,
    val cacheCleared: Boolean = false
)
