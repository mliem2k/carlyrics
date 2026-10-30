package com.mliem.carlyrics

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.car.app.activity.CarAppActivity
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for Spotify Lyrics.
 * Sets up Hilt dependency injection.
 */
@HiltAndroidApp
class SpotifyLyricsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // A resource qualifier cannot gate a manifest component on Android Automotive (the system
        // never applies it), so switch the car launcher on at runtime instead.
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, CarAppActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
