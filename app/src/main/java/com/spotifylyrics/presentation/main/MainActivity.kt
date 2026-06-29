package com.spotifylyrics.presentation.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.spotifylyrics.presentation.navigation.NavGraph
import com.spotifylyrics.presentation.theme.SpotifyLyricsTheme
import com.spotifylyrics.service.foreground.LyricsForegroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LyricsForegroundService.startService(this)
        setContent {
            SpotifyLyricsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    NavGraph(navController = rememberNavController())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            LyricsForegroundService.stopService(this)
        }
    }
}

fun openNotificationSettings(context: android.content.Context) {
    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
}

fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat != null && flat.contains(context.packageName)
}
