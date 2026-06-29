package com.spotifylyrics.service.media

import android.content.Context
import com.spotifylyrics.di.ApplicationScope
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.service.notification.TrackInfoEmitter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackInfoEmitter: TrackInfoEmitter,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val _currentTrackFlow = MutableStateFlow<TrackInfo?>(null)
    val currentTrackFlow: StateFlow<TrackInfo?> = _currentTrackFlow.asStateFlow()

    // Legacy alias kept for compatibility
    val currentTrack: StateFlow<TrackInfo?> = _currentTrackFlow

    private val _playbackPositionFlow = MutableStateFlow(0L)
    val playbackPositionFlow: StateFlow<Long> = _playbackPositionFlow.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var playbackStartWallMs = 0L
    private var playbackStartPositionMs = 0L

    init {
        scope.launch {
            trackInfoEmitter.trackInfoFlow.collect { trackInfo ->
                val wasPlaying = _currentTrackFlow.value?.isPlaying == true
                val isNewTrack = _currentTrackFlow.value?.getKey() != trackInfo.getKey()

                _currentTrackFlow.value = trackInfo
                _isPlaying.value = trackInfo.isPlaying

                if (isNewTrack && trackInfo.isPlaying) {
                    resetPlaybackPosition()
                } else if (!wasPlaying && trackInfo.isPlaying) {
                    resumePositionTracking()
                }
            }
        }
        startPositionTicker()
    }

    private fun startPositionTicker() {
        scope.launch {
            while (true) {
                delay(500)
                if (_isPlaying.value) {
                    val elapsed = System.currentTimeMillis() - playbackStartWallMs
                    _playbackPositionFlow.value = playbackStartPositionMs + elapsed
                }
            }
        }
    }

    private fun resetPlaybackPosition() {
        playbackStartWallMs = System.currentTimeMillis()
        playbackStartPositionMs = 0L
        _playbackPositionFlow.value = 0L
    }

    private fun resumePositionTracking() {
        playbackStartWallMs = System.currentTimeMillis()
        playbackStartPositionMs = _playbackPositionFlow.value
    }

    fun startMonitoring() {}

    fun stopMonitoring() {}

    fun updateTrack(track: String, artist: String, album: String?, isPlaying: Boolean) {
        _currentTrackFlow.value = TrackInfo(
            track = track,
            artist = artist,
            album = album,
            isPlaying = isPlaying
        )
        _isPlaying.value = isPlaying
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        if (_isPlaying.value) resumePositionTracking()
    }

    fun skipToNext() {
        resetPlaybackPosition()
    }

    fun skipToPrevious() {
        resetPlaybackPosition()
    }

    fun seekTo(positionMs: Long) {
        playbackStartWallMs = System.currentTimeMillis()
        playbackStartPositionMs = positionMs
        _playbackPositionFlow.value = positionMs
    }
}
