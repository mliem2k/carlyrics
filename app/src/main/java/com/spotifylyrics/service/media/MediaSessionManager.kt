package com.mliem.carlyrics.service.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
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
import kotlinx.coroutines.flow.first
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

    /** The cover's two main colors, for the backdrop. Null when the player shares no cover. */
    private val _artworkPalette = MutableStateFlow<ArtworkPalette?>(null)
    val artworkPalette: StateFlow<ArtworkPalette?> = _artworkPalette.asStateFlow()
    private var artworkTrackKey = ""

    /** The player's play queue, empty when the player does not publish one. */
    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    /** Queue id of the entry that is playing, or -1. */
    private val _activeQueueId = MutableStateFlow(-1L)
    val activeQueueId: StateFlow<Long> = _activeQueueId.asStateFlow()

    /** Length of the current track, or 0 when the player does not report one. */
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

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
        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) = publish()
        override fun onSessionDestroyed() = publish()
    }

    // Only touched from the main thread (callbacks).
    private var controllers: List<MediaController> = emptyList()

    // Set on the main thread, read by the position timer on another thread.
    @Volatile
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
            while (true) {
                if (!listening) {
                    // Notification access may be granted after startup, so keep retrying.
                    mainHandler.post { tryListen() }
                    delay(RETRY_MS)
                    continue
                }
                // Sleep until something plays: no wakeups while paused or idle.
                _isPlaying.first { it }
                delay(TICK_MS)
                if (anchor.playing) _playbackPositionFlow.value = anchor.estimateAt(SystemClock.elapsedRealtime())
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

    fun skipToQueueItem(id: Long) {
        activeController()?.transportControls?.skipToQueueItem(id)
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
        // Video apps such as YouTube publish sessions too; looking up lyrics for them is wasted work.
        controllers = newControllers.orEmpty().filter { it.packageName in MUSIC_PACKAGES }
        controllers.forEach { it.registerCallback(controllerCallback, mainHandler) }
        publish()
    }

    // ponytail: only embedded bitmaps; players that publish art as a content or web URI give no colors.
    private fun updateArtwork(metadata: MediaMetadata, trackKey: String) {
        // Every pause or resume republishes the same track; read the cover again only for a new one or late art.
        if (trackKey == artworkTrackKey && _artworkPalette.value != null) return
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        artworkTrackKey = trackKey
        _artworkPalette.value = art?.let {
            // Colors only need a thumbnail; the full cover never has to be kept.
            val thumbnail = Bitmap.createScaledBitmap(it, ARTWORK_SIZE_PX, ARTWORK_SIZE_PX, true)
            val pixels = IntArray(ARTWORK_SIZE_PX * ARTWORK_SIZE_PX)
            thumbnail.getPixels(pixels, 0, ARTWORK_SIZE_PX, 0, 0, ARTWORK_SIZE_PX, ARTWORK_SIZE_PX)
            extractPalette(pixels)
        }
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
        val isAd = metadata?.getLong("android.media.metadata.ADVERTISEMENT") == 1L
        if (controller == null || title.isNullOrBlank() || artist.isNullOrBlank() ||
            isAd || looksLikeAdvertisement(title, artist)
        ) {
            anchor = PositionAnchor()
            _currentTrackFlow.value = null
            _isPlaying.value = false
            _playbackPositionFlow.value = 0L
            _durationMs.value = 0L
            _artworkPalette.value = null
            _queue.value = emptyList()
            _activeQueueId.value = -1L
            artworkTrackKey = ""
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
        _durationMs.value = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
        updateArtwork(metadata, "$title|$artist")
        _queue.value = controller.queue.orEmpty().map { item ->
            QueueEntry(
                id = item.queueId,
                title = item.description.title?.toString().orEmpty(),
                subtitle = item.description.subtitle?.toString()
            )
        }
        _activeQueueId.value = state?.activeQueueItemId ?: -1L
        _playbackPositionFlow.value = anchor.estimateAt(SystemClock.elapsedRealtime())
    }

    companion object {
        private const val TAG = "MediaSessionManager"
        private const val ARTWORK_SIZE_PX = 32
        private const val TICK_MS = 250L
        private const val RETRY_MS = 2_000L
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

/** Spotify ad breaks come through as a normal session; some builds omit the ADVERTISEMENT flag. */
internal fun looksLikeAdvertisement(title: String, artist: String): Boolean =
    title.equals("Advertisement", ignoreCase = true) ||
        (title.equals("Spotify", ignoreCase = true) && artist.equals("Advertisement", ignoreCase = true))

/** Music players whose sessions get lyrics. Anything else (YouTube, Netflix, podcasts) is ignored. */
internal val MUSIC_PACKAGES = setOf(
    "com.spotify.music",
    "com.spotify.lite",
    "com.apple.android.music",
    "com.google.android.apps.youtube.music",
    "com.amazon.mp3",
    "com.pandora.android",
    "com.iheart.android",
    "com.soundcloud.android",
    "com.saavn.android",
    "com.miui.player",
    "com.sec.android.app.music",
    "com.sonyericsson.music",
    "com.maxmpz.audioplayer",
    "com.nullsoft.winamp",
    "com.deezer.android.app",
    "com.aspiro.tidal",
    "com.qobuz.music"
)

/** One song in the player's queue. [id] is what the player expects back to jump to it. */
data class QueueEntry(val id: Long, val title: String, val subtitle: String?)
