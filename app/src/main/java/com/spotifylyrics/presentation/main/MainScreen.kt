package com.spotifylyrics.presentation.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.presentation.theme.SpotifyGreen
import com.spotifylyrics.presentation.theme.SpotifyLightGray
import kotlinx.coroutines.launch

/**
 * Main Screen - displays current track info and lyrics
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToLyricsManager: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    val hasNotificationPermission = isNotificationListenerEnabled(context)

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("CarLyrics", color = Color.White) },
                backgroundColor = Color.Black,
                elevation = 0.dp,
                actions = {
                    IconButton(onClick = onNavigateToLyricsManager) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Lyrics Library",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        backgroundColor = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission warning
            if (!hasNotificationPermission) {
                PermissionWarningCard(
                    onOpenSettings = { openNotificationSettings(context) }
                )
            }

            // Track info card
            TrackInfoCard(
                trackInfo = uiState.currentTrack,
                modifier = Modifier.fillMaxWidth()
            )

            // Lyrics card
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }

                uiState.lyrics != null -> {
                    Button(
                        onClick = onNavigateToLyrics,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = SpotifyGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Open Companion View", fontWeight = FontWeight.Bold)
                    }
                    LyricsCard(
                        lyrics = uiState.lyrics!!.plainLyrics,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                uiState.currentTrack != null && uiState.currentTrack!!.isPlaying -> {
                    EmptyLyricsCard(
                        onFetchLyrics = {
                            uiState.currentTrack?.let { viewModel.fetchLyrics(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    NoTrackCard()
                }
            }

            // Error message
            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = Color.Red,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionWarningCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF282828),
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Notification Access Required",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant notification access to detect music",
                style = MaterialTheme.typography.body2,
                color = SpotifyLightGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SpotifyGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TrackInfoCard(
    trackInfo: TrackInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF282828),
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (trackInfo == null || trackInfo.track.isEmpty()) {
                Text(
                    text = "No track playing",
                    style = MaterialTheme.typography.h6,
                    color = SpotifyLightGray
                )
            } else {
                Text(
                    text = trackInfo.track,
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trackInfo.artist,
                    style = MaterialTheme.typography.subtitle1,
                    color = SpotifyLightGray,
                    textAlign = TextAlign.Center
                )
                if (trackInfo.album != null) {
                    Text(
                        text = trackInfo.album,
                        style = MaterialTheme.typography.body2,
                        color = Color(0xFF6A6A6A),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(color = SpotifyGreen)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading lyrics...",
            style = MaterialTheme.typography.body2,
            color = SpotifyLightGray
        )
    }
}

@Composable
fun LyricsCard(
    lyrics: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF282828),
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = lyrics,
                style = MaterialTheme.typography.body1,
                color = SpotifyLightGray
            )
        }
    }
}

@Composable
fun EmptyLyricsCard(
    onFetchLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF282828),
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No lyrics loaded",
                style = MaterialTheme.typography.h6,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onFetchLyrics,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SpotifyGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fetch Lyrics", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NoTrackCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF282828),
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Play music in Spotify or another app",
                style = MaterialTheme.typography.body1,
                color = SpotifyLightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "to see lyrics here",
                style = MaterialTheme.typography.body1,
                color = SpotifyLightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
