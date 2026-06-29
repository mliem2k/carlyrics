package com.spotifylyrics.presentation.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.domain.repository.LyricsRepository
import com.spotifylyrics.domain.repository.TrackRepository
import com.spotifylyrics.service.media.MediaSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Phone Companion Lyrics View
 */
@HiltViewModel
class CompanionLyricsViewModel @Inject constructor(
    private val mediaSessionManager: MediaSessionManager,
    private val lyricsRepository: LyricsRepository,
    private val trackRepository: TrackRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()
    
    private val _currentLyricIndex = MutableStateFlow(0)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()
    
    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()
    
    private val _isNightMode = MutableStateFlow(false)
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()
    
    private val _brightness = MutableStateFlow(1f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()
    
    private val _autoScrollEnabled = MutableStateFlow(true)
    val autoScrollEnabled: StateFlow<Boolean> = _autoScrollEnabled.asStateFlow()
    
    init {
        setupMediaSessionListener()
        observePlaybackUpdates()
    }
    
    private fun setupMediaSessionListener() {
        viewModelScope.launch {
            mediaSessionManager.currentTrackFlow.collect { trackInfo ->
                trackInfo?.let { track ->
                    _uiState.value = _uiState.value.copy(currentTrack = track)
                    fetchLyrics(track.artist, track.title)
                }
            }
        }
    }
    
    private fun observePlaybackUpdates() {
        viewModelScope.launch {
            mediaSessionManager.playbackPositionFlow.collect { positionMs ->
                _playbackPositionMs.value = positionMs
                syncLyricsToPlayback(positionMs)
            }
        }
    }
    
    private fun fetchLyrics(artist: String, title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val lyrics = lyricsRepository.getLyrics(artist, title)
                
                if (lyrics != null) {
                    _uiState.value = _uiState.value.copy(
                        lyrics = lyrics.lines,
                        isLoading = false,
                        error = null
                    )
                    _currentLyricIndex.value = 0
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Lyrics not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun syncLyricsToPlayback(playbackTimeMs: Long) {
        val lyrics = _uiState.value.lyrics
        if (lyrics.isEmpty()) return
        
        var newIndex = 0
        for (i in lyrics.indices) {
            val line = lyrics[i]
            val nextLine = if (i + 1 < lyrics.size) lyrics[i + 1] else null
            
            if (line.timeMs <= playbackTimeMs) {
                if (nextLine == null || playbackTimeMs < nextLine.timeMs) {
                    newIndex = i
                    break
                }
            }
        }
        
        if (_currentLyricIndex.value != newIndex) {
            _currentLyricIndex.value = newIndex
        }
    }
    
    fun toggleAutoScroll() {
        _autoScrollEnabled.value = !_autoScrollEnabled.value
    }
    
    fun toggleNightMode() {
        _isNightMode.value = !_isNightMode.value
    }
    
    fun setBrightness(level: Float) {
        _brightness.value = level.coerceIn(0f, 1f)
    }
    
    fun togglePlayPause() {
        mediaSessionManager.togglePlayPause()
    }
    
    fun skipNext() {
        mediaSessionManager.skipToNext()
    }
    
    fun skipPrevious() {
        mediaSessionManager.skipToPrevious()
    }
    
    fun seekTo(positionMs: Long) {
        mediaSessionManager.seekTo(positionMs)
    }
}

data class CompanionUiState(
    val currentTrack: TrackInfo? = null,
    val lyrics: List<LyricLine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class LyricLine(
    val text: String,
    val timeMs: Long = 0L,
    val durationMs: Long? = null
)
