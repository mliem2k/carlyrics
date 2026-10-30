package com.mliem.carlyrics.presentation.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.domain.repository.LyricsRepository
import com.mliem.carlyrics.data.update.AppUpdater
import com.mliem.carlyrics.domain.repository.SettingsRepository
import com.mliem.carlyrics.service.media.MediaSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the home screen: the current track, its lyrics following playback, the
 * player controls, and the self update status.
 */
@HiltViewModel
class CompanionLyricsViewModel @Inject constructor(
    private val mediaSessionManager: MediaSessionManager,
    private val lyricsRepository: LyricsRepository,
    private val settingsRepository: SettingsRepository,
    val updater: AppUpdater
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()
    
    val isPlaying: StateFlow<Boolean> = mediaSessionManager.isPlaying

    private val _currentLyricIndex = MutableStateFlow(0)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()
    
    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()
    
    init {
        updater.check()
        setupMediaSessionListener()
        observePlaybackUpdates()
    }
    
    private fun setupMediaSessionListener() {
        viewModelScope.launch {
            mediaSessionManager.currentTrackFlow.collect { trackInfo ->
                trackInfo?.let { track ->
                    // Pause/resume re-emits the same track; only a new track needs lyrics.
                    val isNewTrack = _uiState.value.currentTrack?.getKey() != track.getKey()
                    _uiState.value = _uiState.value.copy(currentTrack = track)
                    if (isNewTrack) {
                        _uiState.value = _uiState.value.copy(lyrics = emptyList(), error = null)
                        _currentLyricIndex.value = 0
                        if (settingsRepository.isAutoFetchEnabled().first()) {
                            fetchLyrics(track.artist, track.track)
                        }
                    }
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
    
    /** Looks the current track up again, for the Try again and Fetch lyrics buttons. */
    fun retryLyrics() {
        _uiState.value.currentTrack?.let { fetchLyrics(it.artist, it.track) }
    }

    private fun fetchLyrics(artist: String, title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val trackInfo = com.mliem.carlyrics.domain.model.TrackInfo(
                track = title,
                artist = artist
            )
            lyricsRepository.getLyrics(trackInfo)
                .onSuccess { lyrics ->
                    val lines = lyrics.syncedLyrics?.map { sl ->
                        LyricLine(text = sl.text, timeMs = sl.startTime)
                    } ?: lyrics.plainLyrics.lines()
                        .filter { it.isNotBlank() }
                        .map { LyricLine(text = it) }

                    _uiState.value = _uiState.value.copy(
                        lyrics = lines,
                        isLoading = false,
                        error = null
                    )
                    _currentLyricIndex.value = 0
                    // A paused track emits no new position, so place the line now.
                    syncLyricsToPlayback(_playbackPositionMs.value)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "No lyrics found"
                    )
                }
        }
    }
    
    private fun syncLyricsToPlayback(playbackTimeMs: Long) {
        val lyrics = _uiState.value.lyrics
        // Plain lyrics carry no timestamps, so there is no line to follow.
        if (lyrics.none { it.timeMs > 0 }) return
        
        var newIndex = -1
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

        if (newIndex >= 0 && _currentLyricIndex.value != newIndex) {
            _currentLyricIndex.value = newIndex
        }
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
