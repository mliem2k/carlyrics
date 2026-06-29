package com.mliem.carlyrics.presentation.main

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.presentation.theme.SpotifyGreen
import com.mliem.carlyrics.presentation.theme.SpotifyLightGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToLyricsManager: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val hasNotificationPermission = isNotificationListenerEnabled(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CarLyrics", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
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
        containerColor = Color.Black,
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
            if (!hasNotificationPermission) {
                PermissionWarningCard(
                    onOpenSettings = { openNotificationSettings(context) }
                )
            }

            TrackInfoCard(
                trackInfo = uiState.currentTrack,
                modifier = Modifier.fillMaxWidth()
            )

            val lyrics = uiState.lyrics
            val currentTrack = uiState.currentTrack

            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }

                lyrics != null -> {
                    Button(
                        onClick = onNavigateToLyrics,
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
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
                        lyrics = lyrics.plainLyrics,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                currentTrack != null && currentTrack.isPlaying -> {
                    EmptyLyricsCard(
                        onFetchLyrics = { viewModel.fetchLyrics(currentTrack) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    NoTrackCard()
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Notification Access Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant notification access to detect music",
                style = MaterialTheme.typography.bodyMedium,
                color = SpotifyLightGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (trackInfo == null || trackInfo.track.isEmpty()) {
                Text(
                    text = "No track playing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SpotifyLightGray
                )
            } else {
                Text(
                    text = trackInfo.track,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trackInfo.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = SpotifyLightGray,
                    textAlign = TextAlign.Center
                )
                if (trackInfo.album != null) {
                    Text(
                        text = trackInfo.album,
                        style = MaterialTheme.typography.bodyMedium,
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
            style = MaterialTheme.typography.bodyMedium,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = lyrics,
                style = MaterialTheme.typography.bodyLarge,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No lyrics loaded",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onFetchLyrics,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Play music in Spotify or another app to see lyrics here",
                style = MaterialTheme.typography.bodyLarge,
                color = SpotifyLightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
