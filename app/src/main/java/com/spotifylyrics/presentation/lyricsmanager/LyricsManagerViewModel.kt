package com.spotifylyrics.presentation.lyricsmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.repository.LyricsRepository
import com.spotifylyrics.domain.usecase.ClearCacheUseCase
import com.spotifylyrics.domain.usecase.ImportLrcFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsManagerViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    private val importLrcFileUseCase: ImportLrcFileUseCase,
    private val clearCacheUseCase: ClearCacheUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsManagerUiState())
    val uiState: StateFlow<LyricsManagerUiState> = _uiState.asStateFlow()

    private var allLyrics: List<Lyrics> = emptyList()

    init {
        loadCachedLyrics()
    }

    private fun loadCachedLyrics() {
        viewModelScope.launch {
            lyricsRepository.getAllCachedLyrics().collect { lyrics ->
                allLyrics = lyrics
                applySearch(_uiState.value.searchQuery)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applySearch(query)
    }

    private fun applySearch(query: String) {
        val filtered = if (query.isBlank()) {
            allLyrics
        } else {
            val q = query.trim().lowercase()
            allLyrics.filter {
                it.track.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
        _uiState.value = _uiState.value.copy(lyrics = filtered)
    }

    fun importLrc(track: String, artist: String, lrcContent: String) {
        if (track.isBlank() || artist.isBlank() || lrcContent.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importResult = null)
            importLrcFileUseCase(track.trim(), artist.trim(), lrcContent)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importResult = "Imported: ${artist.trim()} – ${track.trim()}"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importResult = "Error: ${e.message}"
                    )
                }
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            clearCacheUseCase()
            _uiState.value = _uiState.value.copy(
                lyrics = emptyList(),
                importResult = "Cache cleared"
            )
        }
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(importResult = null)
    }
}

data class LyricsManagerUiState(
    val lyrics: List<Lyrics> = emptyList(),
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val importResult: String? = null
)
