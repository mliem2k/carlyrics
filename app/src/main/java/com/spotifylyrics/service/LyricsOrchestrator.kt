package com.mliem.carlyrics.service

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.domain.repository.LyricsRepository
import com.mliem.carlyrics.presentation.widget.WidgetStateManager
import com.mliem.carlyrics.service.media.MediaSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private var trackChangesJob: Job? = null
    private var playbackPositionJob: Job? = null
    private var activeConsumers = 0

    /**
     * Reference counted like MediaSessionManager.startMonitoring(): both the
     * phone foreground service and an Android Auto session call this
     * independently, and the engine must keep running as long as at least
     * one of them is active.
     */
    fun start() {
        activeConsumers++
        if (trackChangesJob != null) return
        trackChangesJob = observeTrackChanges()
        playbackPositionJob = observePlaybackPosition()
    }

    /**
     * Releases one consumer's need for the engine. Only actually stops the
     * collectors once every consumer that called start() has called this too.
     */
    fun stop() {
        activeConsumers = (activeConsumers - 1).coerceAtLeast(0)
        if (activeConsumers > 0) return
        trackChangesJob?.cancel()
        trackChangesJob = null
        playbackPositionJob?.cancel()
        playbackPositionJob = null
    }

    private fun observeTrackChanges(): Job = scope.launch {
        // collectLatest: cancels a still in-flight getLyrics() for the
        // previous track the moment a new one arrives, so a slow fetch for
        // track A can never complete after track B has already started and
        // overwrite the widget/notification with A's stale data.
        mediaSessionManager.currentTrackFlow.collectLatest { trackInfo ->
            trackInfo ?: return@collectLatest
            _currentLyrics.value = null
            _currentLyricLine.value = ""

            lyricsRepository.getLyrics(TrackInfo(track = trackInfo.track, artist = trackInfo.artist))
                .onSuccess { lyrics ->
                    _currentLyrics.value = lyrics
                    updateWidget(trackInfo.track, trackInfo.artist, "", trackInfo.isPlaying)
                }
        }
    }

    private fun observePlaybackPosition(): Job = scope.launch {
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
