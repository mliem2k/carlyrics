package com.mliem.carlyrics.service.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.mliem.carlyrics.di.ApplicationScope
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.service.notification.MusicNotificationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import android.media.session.MediaSessionManager as SystemMediaSessionManager

/**
 * Follows the active system media session (Spotify first) and exposes its track,
 * play state and real playback position. Reading the session needs notification
 * access, which the user already grants for [MusicNotificationListener].
 */
@Singleton
class MediaSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val _currentTrackFlow = MutableStateFlow<TrackInfo?>(null)
    val currentTrackFlow: StateFlow<TrackInfo?> = _currentTrackFlow.asStateFlow()

    private val _playbackPositionFlow = MutableStateFlow(0L)
    val playbackPositionFlow: StateFlow<Long> = _playbackPositionFlow.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val systemManager = context.getSystemService(SystemMediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MusicNotificationListener::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sessionsListener =
        SystemMediaSessionManager.OnActiveSessionsChangedListener { bind(it) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = publish()
    }

    // Only touched from the main thread (callbacks) or after listening starts.
    private var controllers: List<MediaController> = emptyList()
    private var listening = false

    @Volatile
    private var anchor = PositionAnchor()
    private var tickerJob: Job? = null
    private var activeConsumers = 0

    /**
     * Reference counted: the phone foreground service and an Android Auto
     * session both call this, and monitoring must outlive either one alone.
     */
    fun startMonitoring() {
        activeConsumers++
        if (tickerJob != null) return
        tickerJob = scope.launch {
            var ticksSinceRetry = 0
            while (true) {
                delay(TICK_MS)
                if (!listening) {
                    // Notification access may be granted after startup, so keep retrying.
                    if (ticksSinceRetry++ % RETRY_EVERY_TICKS == 0) mainHandler.post { tryListen() }
                } else if (anchor.playing) {
                    _playbackPositionFlow.value = anchor.estimateAt(SystemClock.elapsedRealtime())
                }
            }
        }
    }

    fun stopMonitoring() {
        activeConsumers = (activeConsumers - 1).coerceAtLeast(0)
        if (activeConsumers > 0) return
        tickerJob?.cancel()
        tickerJob = null
        mainHandler.post {
            if (listening) systemManager.removeOnActiveSessionsChangedListener(sessionsListener)
            listening = false
            bind(emptyList())
        }
    }

    fun togglePlayPause() {
        val controls = activeController()?.transportControls ?: return
        if (_isPlaying.value) controls.pause() else controls.play()
    }

    fun skipToNext() {
        activeController()?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        activeController()?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        activeController()?.transportControls?.seekTo(positionMs) ?: return
        // Show the jump now; the session's own state update confirms it right after.
        anchor = anchor.copy(positionMs = positionMs, updatedAtMs = SystemClock.elapsedRealtime())
        _playbackPositionFlow.value = positionMs
    }

    private fun tryListen() {
        if (listening) return
        try {
            systemManager.addOnActiveSessionsChangedListener(
                sessionsListener, listenerComponent, mainHandler
            )
            listening = true
            bind(systemManager.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification access not granted yet")
        }
    }

    private fun bind(newControllers: List<MediaController>?) {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = newControllers.orEmpty()
        controllers.forEach { it.registerCallback(controllerCallback, mainHandler) }
        publish()
    }

    /** Playing session wins, then Spotify, then whatever is first. */
    private fun activeController(): MediaController? =
        controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.packageName in SPOTIFY_PACKAGES }
            ?: controllers.firstOrNull()

    private fun publish() {
        val controller = activeController()
        val metadata = controller?.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        if (controller == null || title.isNullOrBlank() || artist.isNullOrBlank()) {
            anchor = PositionAnchor()
            _currentTrackFlow.value = null
            _isPlaying.value = false
            _playbackPositionFlow.value = 0L
            return
        }

        val state = controller.playbackState
        val playing = state?.state == PlaybackState.STATE_PLAYING
        anchor = PositionAnchor(
            positionMs = state?.position ?: 0L,
            updatedAtMs = state?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed ?: 1f,
            playing = playing
        )
        _currentTrackFlow.value = TrackInfo(
            track = title,
            artist = artist,
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
            isPlaying = playing
        )
        _isPlaying.value = playing
        _playbackPositionFlow.value = anchor.estimateAt(SystemClock.elapsedRealtime())
    }

    companion object {
        private const val TAG = "MediaSessionManager"
        private const val TICK_MS = 250L
        private const val RETRY_EVERY_TICKS = 8
        private val SPOTIFY_PACKAGES = setOf("com.spotify.music", "com.spotify.lite")
    }
}

/**
 * Last position the session reported, and when ([SystemClock.elapsedRealtime] clock,
 * as PlaybackState.getLastPositionUpdateTime uses). Position between reports is extrapolated.
 */
internal data class PositionAnchor(
    val positionMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val speed: Float = 1f,
    val playing: Boolean = false
) {
    fun estimateAt(nowMs: Long): Long {
        // PlaybackState.PLAYBACK_POSITION_UNKNOWN is negative.
        val base = positionMs.coerceAtLeast(0L)
        if (!playing) return base
        return base + ((nowMs - updatedAtMs).coerceAtLeast(0L) * speed).toLong()
    }
}
