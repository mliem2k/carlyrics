package com.mliem.carlyrics.service.auto.session

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mliem.carlyrics.service.LyricsOrchestrator
import com.mliem.carlyrics.service.auto.screen.CarLyricsScreen
import com.mliem.carlyrics.service.auto.screen.CarMainScreen
import com.mliem.carlyrics.service.media.MediaSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CarSessionEntryPoint {
    fun lyricsOrchestrator(): LyricsOrchestrator
    fun mediaSessionManager(): MediaSessionManager
}

class CarLyricsSession : Session() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lyricsScreen: CarLyricsScreen? = null
    private var liveDataJob: Job? = null

    override fun onCreateScreen(intent: Intent): Screen {
        val entryPoint = EntryPointAccessors
            .fromApplication(carContext.applicationContext, CarSessionEntryPoint::class.java)
        val orchestrator = entryPoint.lyricsOrchestrator()
        val mediaManager = entryPoint.mediaSessionManager()

        val screen = CarLyricsScreen(carContext)
        lyricsScreen = screen

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Idempotent/reference counted: keeps the engine alive for
                // Android Auto even if the phone app was never opened, and
                // does not double count against a phone session already
                // running it.
                mediaManager.startMonitoring()
                orchestrator.start()
                liveDataJob = observeLiveData(orchestrator, mediaManager, screen)
            }
            override fun onStop(owner: LifecycleOwner) {
                // Cancel only the previous onStart's collector job, not the
                // whole session scope, so repeated screen dim/wake cycles
                // don't accumulate one leaked collector per cycle.
                liveDataJob?.cancel()
                liveDataJob = null
                mediaManager.stopMonitoring()
                orchestrator.stop()
            }
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
                lyricsScreen = null
            }
        })

        return CarMainScreen(carContext)
    }

    private fun observeLiveData(
        orchestrator: LyricsOrchestrator,
        mediaManager: MediaSessionManager,
        screen: CarLyricsScreen
    ): Job = scope.launch {
        combine(
            orchestrator.currentLyrics,
            mediaManager.currentTrackFlow,
            mediaManager.playbackPositionFlow
        ) { lyrics, track, positionMs ->
            Triple(track, lyrics, positionMs)
        }.collect { (track, lyrics, positionMs) ->
            screen.update(track, lyrics, positionMs)
        }
    }
}
