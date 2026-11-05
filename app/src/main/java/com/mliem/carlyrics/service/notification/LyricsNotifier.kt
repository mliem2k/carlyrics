package com.mliem.carlyrics.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mliem.carlyrics.di.ApplicationScope
import com.mliem.carlyrics.presentation.main.MainActivity
import com.mliem.carlyrics.service.LyricsOrchestrator
import com.mliem.carlyrics.service.media.MediaSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows the current lyric line in the status bar while a music app plays, and takes the
 * notification down after [IDLE_MS] of silence so nothing lingers (or runs) when idle.
 */
@Singleton
class LyricsNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaSessionManager: MediaSessionManager,
    private val orchestrator: LyricsOrchestrator,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private var job: Job? = null

    fun start() {
        if (job != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "CarLyrics", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Live lyrics in the status bar" }
        )
        job = scope.launch {
            combine(
                mediaSessionManager.currentTrackFlow,
                orchestrator.currentLyricLine,
                mediaSessionManager.isPlaying
            ) { track, line, playing -> Triple(track, line, playing) }
                .collectLatest { (track, line, playing) ->
                    if (track != null && playing) {
                        show("${track.artist} - ${track.track}", line.ifBlank { "Fetching lyrics..." })
                    } else {
                        // collectLatest drops this wait the moment playback resumes.
                        delay(IDLE_MS)
                        notificationManager.cancel(NOTIFICATION_ID)
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun show(title: String, text: String) {
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(open)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
    }

    private companion object {
        const val CHANNEL_ID = "lyrics_channel"
        const val NOTIFICATION_ID = 1001
        const val IDLE_MS = 2 * 60 * 1000L
    }
}
