package com.spotifylyrics.presentation.lyricsmanager

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for LyricsManagerScreen
 */
@HiltViewModel
class LyricsManagerViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(LyricsManagerUiState())
    val uiState: StateFlow<LyricsManagerUiState> = _uiState.asStateFlow()
}

/**
 * UI state for LyricsManagerScreen
 */
data class LyricsManagerUiState(
    val isLoading: Boolean = false
)
