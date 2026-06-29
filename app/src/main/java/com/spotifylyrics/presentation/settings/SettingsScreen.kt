package com.spotifylyrics.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotifylyrics.presentation.theme.SpotifyGreen
import com.spotifylyrics.presentation.theme.SpotifyLightGray

/**
 * Settings Screen - app preferences and cache management
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                backgroundColor = Color.Black,
                elevation = 0.dp
            )
        },
        backgroundColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Lyrics sources
            SettingsSection(title = "Lyrics Sources") {
                SwitchSetting(
                    title = "LRCLIB (synced)",
                    description = "Free synced lyrics — tried first for timestamped lines",
                    checked = uiState.lrclibEnabled,
                    onCheckedChange = { viewModel.toggleLrclib(it) }
                )
                SwitchSetting(
                    title = "Genius",
                    description = "Plain lyrics fallback via Genius.com",
                    checked = uiState.geniusEnabled,
                    onCheckedChange = { viewModel.toggleGenius(it) }
                )
                SwitchSetting(
                    title = "Musixmatch",
                    description = "Plain lyrics fallback via Musixmatch",
                    checked = uiState.musixmatchEnabled,
                    onCheckedChange = { viewModel.toggleMusixmatch(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cache settings
            SettingsSection(title = "Cache") {
                SwitchSetting(
                    title = "Auto-fetch Lyrics",
                    description = "Automatically fetch lyrics when track changes",
                    checked = uiState.autoFetchEnabled,
                    onCheckedChange = { viewModel.toggleAutoFetch(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.clearCache()
                        viewModel.cacheClearedHandled()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF282828),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Cache", color = Color.White)
                }

                if (uiState.cacheCleared) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cache cleared!",
                        color = SpotifyGreen,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = SpotifyLightGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF282828),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.body2,
                color = SpotifyLightGray
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SpotifyGreen,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = SpotifyGreen.copy(alpha = 0.5f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}
