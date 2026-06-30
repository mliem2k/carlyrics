package com.mliem.carlyrics.service.notification

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mliem.carlyrics.domain.model.TrackInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that listens to music app notifications to detect currently playing track.
 * This is a native Kotlin implementation without Flutter dependencies.
 */
@AndroidEntryPoint
class MusicNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "MusicNotificationListener"

        // List of supported music app package names
        private val MUSIC_APPS = setOf(
            "com.spotify.music",
            "com.spotify.lite",
            "com.apple.android.music",
            "com.google.android.apps.youtube.music",
            "com.amazon.mp3",
            "com.pandora.android",
            "com.iheart.android",
            "com.tunebaseweb.android",
            "com.soundcloud.android",
            "com.saavn.android",
            "com.miui.player",
            "com.sec.android.app.music",
            "com.sonyericsson.music",
            "com.rdio.android",
            "com.slacker.radio",
            "com.jamendo.www",
            "com.kmplayer",
            "com.vt.AndroidMP",
            "com.maxmpz.audioplayer",
            "com.nullsoft.winamp",
            "com.deezer.android.app"
        )

    }

    @Inject
    lateinit var trackEmitter: TrackInfoEmitter

    private var lastTrackInfo: TrackInfo? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MusicNotificationListener created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MusicNotificationListener destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        try {
            val packageName = sbn.packageName
            if (!MUSIC_APPS.contains(packageName)) {
                return
            }

            val notification = sbn.notification ?: return
            val extras = notification.extras

            val track = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT_LINES)?.toString() ?: ""
            val album = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

            // Get media session metadata for more accurate info
            val mediaTitle = extras.getCharSequence("android.title")?.toString()
            val mediaArtist = extras.getCharSequence("android.text")?.toString()
            val mediaAlbum = extras.getCharSequence("android.subtext")?.toString()

            val finalTrack = mediaTitle ?: track
            val finalArtist = mediaArtist ?: artist
            val finalAlbum = mediaAlbum ?: album

            val isPlaying = isMediaPlaying(notification, extras)

            if (finalTrack.isNotEmpty() && finalArtist.isNotEmpty()) {
                val currentInfo = TrackInfo(
                    track = finalTrack,
                    artist = finalArtist,
                    album = finalAlbum,
                    isPlaying = isPlaying
                )

                // Only notify if track info changed
                if (currentInfo != lastTrackInfo) {
                    lastTrackInfo = currentInfo
                    Log.d(TAG, "Track detected: $finalTrack by $finalArtist (playing: $isPlaying)")

                    if (isPlaying) {
                        notifyTrackChanged(currentInfo)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification: ${e.message}")
        }
    }

    private fun isMediaPlaying(notification: Notification, extras: android.os.Bundle): Boolean {
        // Check for action icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            notification.actions?.forEach { action ->
                val actionTitle = action.title?.toString() ?: ""
                // If pause action exists, media is playing
                if (actionTitle.equals("Pause", ignoreCase = true) ||
                    actionTitle.equals("⏸", ignoreCase = true) ||
                    actionTitle.contains("pause", ignoreCase = true)) {
                    return true
                }
            }
        }

        // Check extras for playback state
        val mediaSession = extras.getString("android.mediaSession")
        if (mediaSession != null) {
            return true
        }

        // Check if notification has media style
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val template = extras.getString("android.template")
            if (template != null) {
                return true
            }
        }

        return false
    }

    private fun notifyTrackChanged(trackInfo: TrackInfo) {
        trackEmitter.emitTrackInfo(trackInfo)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}

/**
 * Singleton emitter for track information using Kotlin Flow
 */
@Singleton
class TrackInfoEmitter @Inject constructor() {
    private val _trackInfoFlow = MutableSharedFlow<TrackInfo>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val trackInfoFlow: Flow<TrackInfo> = _trackInfoFlow.asSharedFlow()

    fun emitTrackInfo(trackInfo: TrackInfo) {
        _trackInfoFlow.tryEmit(trackInfo)
    }
}
