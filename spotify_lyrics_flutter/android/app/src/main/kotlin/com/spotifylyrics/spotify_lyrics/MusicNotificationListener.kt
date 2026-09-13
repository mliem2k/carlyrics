package com.spotifylyrics.spotify_lyrics

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation

class MusicNotificationListener : NotificationListenerService() {
    companion object {
        private const val TAG = "MusicNotificationListener"
        private const val CHANNEL = "music_detection"

        private var instance: MusicNotificationListener? = null
        private var methodChannel: MethodChannel? = null
        private var flutterEngine: FlutterEngine? = null
        private var lastTrackInfo: TrackInfo? = null

        fun getInstance(): MusicNotificationListener? = instance

        fun setMethodChannel(channel: MethodChannel) {
            methodChannel = channel
        }

        fun setFlutterEngine(engine: FlutterEngine) {
            flutterEngine = engine
        }
    }

    private val musicApps = setOf(
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
        "com Deezer"
    )

    data class TrackInfo(
        val track: String,
        val artist: String,
        val album: String?,
        val isPlaying: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "MusicNotificationListener created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "MusicNotificationListener destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        try {
            val packageName = sbn.packageName
            if (!musicApps.contains(packageName)) {
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
                val currentInfo = TrackInfo(finalTrack, finalArtist, finalAlbum, isPlaying)

                // Only notify if track info changed
                if (currentInfo != lastTrackInfo) {
                    lastTrackInfo = currentInfo
                    Log.d(TAG, "Track detected: $finalTrack by $finalArtist (playing: $isPlaying)")

                    if (isPlaying) {
                        notifyTrackChanged(finalTrack, finalArtist, finalAlbum)
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

    private fun notifyTrackChanged(track: String, artist: String, album: String?) {
        val trackData = mapOf(
            "track" to track,
            "artist" to artist,
            "album" to (album ?: ""),
            "isPlaying" to true
        )

        // Send to Flutter via MethodChannel
        methodChannel?.invokeMethod("onTrackChanged", trackData)
            ?: Log.w(TAG, "MethodChannel not initialized, can't send track data")

        // Also try to send via MainActivity's channel
        try {
            val intent = Intent("com.spotifylyrics.TRACK_CHANGED")
            intent.putExtra("track", track)
            intent.putExtra("artist", artist)
            intent.putExtra("album", album ?: "")
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
