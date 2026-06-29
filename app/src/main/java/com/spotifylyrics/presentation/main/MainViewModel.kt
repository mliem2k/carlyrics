package com.spotifylyrics.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.domain.usecase.GetCurrentTrackUseCase
import com.spotifylyrics.domain.usecase.GetLyricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getLyricsUseCase: GetLyricsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeCurrentTrack()
    }

    private fun observeCurrentTrack() {
        viewModelScope.launch {
            getCurrentTrackUseCase().collect { trackInfo ->
                _uiState.value = _uiState.value.copy(
                    currentTrack = trackInfo
                )

                // Auto-fetch lyrics when track changes
                if (trackInfo != null && trackInfo.isPlaying) {
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
    val lyrics: com.spotifylyrics.domain.model.Lyrics? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
