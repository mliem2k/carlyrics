package com.mliem.carlyrics.service.notification

import android.service.notification.NotificationListenerService
import com.mliem.carlyrics.service.LyricsOrchestrator
import com.mliem.carlyrics.service.media.MediaSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Enabling notification access for this service is what lets [MediaSessionManager] read media sessions.
 * The system keeps it bound while access is on, so it also hosts the engine: no foreground service is
 * needed, and everything is event driven (nothing runs while no music plays).
 */
@AndroidEntryPoint
class MusicNotificationListener : NotificationListenerService() {

    @Inject lateinit var mediaSessionManager: MediaSessionManager
    @Inject lateinit var orchestrator: LyricsOrchestrator
    @Inject lateinit var notifier: LyricsNotifier

    private var running = false

    override fun onListenerConnected() {
        if (running) return
        running = true
        mediaSessionManager.startMonitoring()
        orchestrator.start()
        notifier.start()
    }

    override fun onListenerDisconnected() = shutDown()

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    private fun shutDown() {
        if (!running) return
        running = false
        notifier.stop()
        orchestrator.stop()
        mediaSessionManager.stopMonitoring()
    }
}
