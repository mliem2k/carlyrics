package com.mliem.carlyrics

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for Spotify Lyrics.
 * Sets up Hilt dependency injection.
 */
@HiltAndroidApp
class SpotifyLyricsApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
