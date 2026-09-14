package com.mliem.carlyrics.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.domain.repository.SettingsRepository
import com.mliem.carlyrics.domain.usecase.GetCurrentTrackUseCase
import com.mliem.carlyrics.domain.usecase.GetLyricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainScreen
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCurrentTrackUseCase: GetCurrentTrackUseCase,
    private val getLyricsUseCase: GetLyricsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeCurrentTrack()
    }

    private fun observeCurrentTrack() {
        viewModelScope.launch {
            getCurrentTrackUseCase().collect { trackInfo ->
                // Clear the previous track's lyrics/error the moment the track
                // identity changes (including changing to no track at all), so
                // MainScreen never pairs a new track with a stale lyrics card.
                val trackChanged = trackInfo?.getKey() != _uiState.value.currentTrack?.getKey()
                _uiState.value = _uiState.value.copy(
                    currentTrack = trackInfo,
                    lyrics = if (trackChanged) null else _uiState.value.lyrics,
                    error = if (trackChanged) null else _uiState.value.error
                )

                // Auto-fetch lyrics when track changes, unless the user disabled it
                if (trackInfo != null && trackInfo.isPlaying && settingsRepository.isAutoFetchEnabled().first()) {
                    fetchLyrics(trackInfo)
                }
            }
        }
    }

    fun fetchLyrics(trackInfo: TrackInfo) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = getLyricsUseCase(trackInfo)

            result.onSuccess { lyrics ->
                _uiState.value = _uiState.value.copy(
                    lyrics = lyrics,
                    isLoading = false,
                    error = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for MainScreen
 */
data class MainUiState(
    val currentTrack: TrackInfo? = null,
    val lyrics: com.mliem.carlyrics.domain.model.Lyrics? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
