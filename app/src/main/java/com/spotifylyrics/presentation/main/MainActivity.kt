package com.mliem.carlyrics.presentation.main

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.mliem.carlyrics.presentation.navigation.NavGraph
import com.mliem.carlyrics.presentation.theme.CarLyricsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 13+ hides the status-bar lyrics until this is granted.
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            .launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            CarLyricsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(navController = rememberNavController())
                }
            }
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
