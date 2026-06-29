package com.spotifylyrics.presentation.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifylyrics.domain.usecase.ClearCacheUseCase
import com.spotifylyrics.domain.usecase.GetLyricsUseCase
import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.domain.repository.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for LyricsScreen
 */
@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val getLyricsUseCase: GetLyricsUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val lyricsRepository: LyricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    init {
        loadCachedLyrics()
    }

    private fun loadCachedLyrics() {
        viewModelScope.launch {
            lyricsRepository.getAllCachedLyrics().collect { lyricsList ->
                _uiState.value = _uiState.value.copy(cachedLyrics = lyricsList)
            }
        }
    }

    fun searchLyrics(query: String) {
        viewModelScope.launch {
            lyricsRepository.searchLyrics(query).collect { results ->
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
        }
    }

    fun clearCache(expiredOnly: Boolean = false) {
        viewModelScope.launch {
            clearCacheUseCase(expiredOnly)
        }
    }
}

/**
 * UI state for LyricsScreen
 */
data class LyricsUiState(
    val currentLyrics: Lyrics? = null,
    val cachedLyrics: List<Lyrics> = emptyList(),
    val searchResults: List<Lyrics> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
