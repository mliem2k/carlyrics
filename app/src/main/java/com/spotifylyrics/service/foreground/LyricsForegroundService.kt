package com.spotifylyrics.service.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.spotifylyrics.presentation.main.MainActivity
import com.spotifylyrics.service.LyricsOrchestrator
import com.spotifylyrics.service.media.MediaSessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LyricsForegroundService : Service() {

    @Inject lateinit var lyricsOrchestrator: LyricsOrchestrator
    @Inject lateinit var mediaSessionManager: MediaSessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "lyrics_foreground_channel"
        private const val CHANNEL_NAME = "CarLyrics"

        fun startService(context: Context) {
            val intent = Intent(context, LyricsForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, LyricsForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("CarLyrics", "Listening for music…"))
        lyricsOrchestrator.start()
        observeForNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeForNotification() {
        scope.launch {
            combine(
                mediaSessionManager.currentTrackFlow,
                lyricsOrchestrator.currentLyricLine
            ) { track, lyric -> Pair(track, lyric) }
                .collect { (track, lyric) ->
                    val title = track?.let { "${it.artist} – ${it.track}" } ?: "CarLyrics"
                    val text = lyric.ifBlank {
                        if (track != null) "Fetching lyrics…" else "Listening for music…"
                    }
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(title, text))
                }
        }
    }

    private fun buildNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Live lyrics in the status bar" }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
