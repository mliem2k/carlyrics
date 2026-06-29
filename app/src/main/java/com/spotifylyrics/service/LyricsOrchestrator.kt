package com.mliem.carlyrics.service

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.domain.repository.LyricsRepository
import com.mliem.carlyrics.presentation.widget.WidgetStateManager
import com.mliem.carlyrics.service.media.MediaSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsOrchestrator @Inject constructor(
    private val mediaSessionManager: MediaSessionManager,
    private val lyricsRepository: LyricsRepository,
    private val widgetStateManager: WidgetStateManager,
    @com.mliem.carlyrics.di.ApplicationScope private val scope: CoroutineScope
) {
    private val _currentLyrics = MutableStateFlow<Lyrics?>(null)
    val currentLyrics: StateFlow<Lyrics?> = _currentLyrics.asStateFlow()

    private val _currentLyricLine = MutableStateFlow("")
    val currentLyricLine: StateFlow<String> = _currentLyricLine.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        observeTrackChanges()
        observePlaybackPosition()
    }

    private fun observeTrackChanges() {
        scope.launch {
            mediaSessionManager.currentTrackFlow.collect { trackInfo ->
                trackInfo ?: return@collect
                _currentLyrics.value = null
                _currentLyricLine.value = ""

                lyricsRepository.getLyrics(TrackInfo(track = trackInfo.track, artist = trackInfo.artist))
                    .onSuccess { lyrics ->
                        _currentLyrics.value = lyrics
                        updateWidget(trackInfo.track, trackInfo.artist, "", trackInfo.isPlaying)
                    }
            }
        }
    }

    private fun observePlaybackPosition() {
        scope.launch {
            combine(
                mediaSessionManager.playbackPositionFlow,
                _currentLyrics,
                mediaSessionManager.currentTrackFlow,
                mediaSessionManager.isPlaying
            ) { positionMs, lyrics, track, playing ->
                val line = lyrics?.getCurrentLyric(positionMs) ?: ""
                Triple(track, line, playing)
            }.collect { (track, line, playing) ->
                if (line != _currentLyricLine.value) {
                    _currentLyricLine.value = line
                    track?.let {
                        updateWidget(it.track, it.artist, line, playing)
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(
        track: String,
        artist: String,
        lyricLine: String,
        isPlaying: Boolean
    ) {
        runCatching {
            widgetStateManager.update(track, artist, lyricLine, isPlaying)
        }
    }
}
