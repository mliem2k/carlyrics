package com.spotifylyrics.spotify_lyrics

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel

class MainActivity : FlutterActivity() {
    private val TAG = "MainActivity"
    private val COMM_CHANNEL = "spotify_lyrics/communication"
    private val MUSIC_CHANNEL = "music_detection"
    private val EVENT_CHANNEL = "spotify_lyrics/events"

    private var commMethodChannel: MethodChannel? = null
    private var musicMethodChannel: MethodChannel? = null
    private var eventChannel: EventChannel? = null
    private var eventSink: io.flutter.plugin.common.EventChannel.EventSink? = null

    private val PERMISSION_REQUEST_CODE = 1001

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Setup communication channel
        commMethodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, COMM_CHANNEL)
        commMethodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "initialize" -> {
                    initializeMusicDetection()
                    result.success(mapOf(
                        "status" to "initialized",
                        "hasNotificationPermission" to hasNotificationListenerPermission()
                    ))
                }
                "getDeviceInfo" -> {
                    result.success(mapOf(
                        "platform" to "Android",
                        "version" to Build.VERSION.RELEASE,
                        "sdk" to Build.VERSION.SDK_INT,
                        "hasNotificationPermission" to hasNotificationListenerPermission(),
                        "isNotificationListenerEnabled" to isNotificationListenerEnabled()
                    ))
                }
                "requestNotificationPermission" -> {
                    requestNotificationListenerPermission()
                    result.success(null)
                }
                "enableMockMode" -> {
                    MockMusicDetectionService.startMockDetection { track, artist ->
                        sendTrackToFlutter(track, artist)
                    }
                    result.success(true)
                }
                "disableMockMode" -> {
                    MockMusicDetectionService.stopMockDetection()
                    result.success(true)
                }
                "checkPermissionStatus" -> {
                    result.success(isNotificationListenerEnabled())
                }
                "openNotificationSettings" -> {
                    openNotificationListenerSettings()
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        // Setup music detection channel
        musicMethodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, MUSIC_CHANNEL)
        musicMethodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "isSpotifyInstalled" -> {
                    result.success(isSpotifyInstalled())
                }
                "startMediaDetection" -> {
                    result.success(true)
                }
                "stopDetection" -> {
                    MockMusicDetectionService.stopMockDetection()
                    result.success(true)
                }
                "getCurrentlyPlaying" -> {
                    result.success(MockMusicDetectionService.getCurrentTrack())
                }
                "getInstalledMusicApps" -> {
                    result.success(getInstalledMusicApps())
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        // Setup event channel for continuous updates
        eventChannel = EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
        eventChannel?.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
                Log.d(TAG, "Event channel listening")
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
                Log.d(TAG, "Event channel cancelled")
            }
        })

        // Initialize the MusicNotificationListener with our method channel
        MusicNotificationListener.setMethodChannel(musicMethodChannel!!)
        MusicNotificationListener.setFlutterEngine(flutterEngine)

        Log.d(TAG, "Flutter engine configured")
    }

    private fun initializeMusicDetection() {
        // Check if notification listener permission is granted
        if (!isNotificationListenerEnabled()) {
            Log.w(TAG, "Notification listener permission not granted")
        } else {
            Log.d(TAG, "Notification listener permission granted")
        }
    }

    private fun isSpotifyInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.spotify.music", 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun hasNotificationListenerPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return flat != null && flat.contains(packageName)
    }

    private fun requestNotificationListenerPermission() {
        if (!isNotificationListenerEnabled()) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback for some devices
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Could not open settings: ${e2.message}")
            }
        }
    }

    private fun sendTrackToFlutter(track: String, artist: String) {
        val trackData = mapOf(
            "track" to track,
            "artist" to artist,
            "isPlaying" to true
        )

        // Send via both channels for compatibility
        musicMethodChannel?.invokeMethod("onTrackChanged", trackData)
        commMethodChannel?.invokeMethod("onTrackChanged", trackData)
        eventSink?.success(trackData)
    }

    private fun getInstalledMusicApps(): List<String> {
        val musicApps = mutableListOf<String>()
        val apps = listOf(
            "com.spotify.music" to "Spotify",
            "com.apple.android.music" to "Apple Music",
            "com.google.android.apps.youtube.music" to "YouTube Music",
            "com.amazon.mp3" to "Amazon Music",
            "com.pandora.android" to "Pandora",
            "com.iheart.android" to "iHeartRadio",
            "com.soundcloud.android" to "SoundCloud",
            "com.saavn.android" to "JioSaavn"
        )

        apps.forEach { (packageName, appName) ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                musicApps.add(appName)
            } catch (e: PackageManager.NameNotFoundException) {
                // App not installed
            }
        }

        return musicApps
    }

    override fun onDestroy() {
        super.onDestroy()
        MockMusicDetectionService.stopMockDetection()
        eventSink = null
    }
}

// Mock service for testing
object MockMusicDetectionService {
    private val mockTracks = listOf(
        mapOf("track" to "Blinding Lights", "artist" to "The Weeknd"),
        mapOf("track" to "Shape of You", "artist" to "Ed Sheeran"),
        mapOf("track" to "Someone Like You", "artist" to "Adele"),
        mapOf("track" to "Bad Guy", "artist" to "Billie Eilish"),
        mapOf("track" to "Uptown Funk", "artist" to "Mark Ronson ft. Bruno Mars")
    )
    private var currentIndex = 0
    private var timer: java.util.Timer? = null
    private var onTrackChangedCallback: ((String, String) -> Unit)? = null

    fun startMockDetection(onTrackChanged: (String, String) -> Unit) {
        onTrackChangedCallback = onTrackChanged
        timer?.cancel()
        timer = java.util.Timer().apply {
            scheduleAtFixedRate(object : java.util.TimerTask() {
                override fun run() {
                    val track = mockTracks[currentIndex]
                    onTrackChanged(track["track"] ?: "", track["artist"] ?: "")
                    currentIndex = (currentIndex + 1) % mockTracks.size
                }
            }, 0, 10000) // Change track every 10 seconds
        }
    }

    fun stopMockDetection() {
        timer?.cancel()
        timer = null
    }

    fun getCurrentTrack(): Map<String, Any>? {
        return if (timer != null) {
            mapOf(
                "track" to (mockTracks[currentIndex]["track"] ?: ""),
                "artist" to (mockTracks[currentIndex]["artist"] ?: ""),
                "isPlaying" to true
            )
        } else {
            null
        }
    }
}
