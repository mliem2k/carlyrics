package com.mliem.carlyrics.presentation.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mliem.carlyrics.BuildConfig
import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.presentation.update.UpdateCard
import com.mliem.carlyrics.presentation.update.needsAttention

private val Panel = Color(0xFF1A1A1A)
private const val PREVIEW_LINES = 6

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToLyricsManager: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updater.state.collectAsState()
    val context = LocalContext.current

    var hasNotificationAccess by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    // The grant happens in system settings, so re-check on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationAccess = isNotificationListenerEnabled(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopBar(onNavigateToLyricsManager, onNavigateToSettings)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (updateState.needsAttention()) {
                UpdateCard(
                    state = updateState,
                    currentVersion = BuildConfig.VERSION_NAME,
                    onCheck = viewModel.updater::check,
                    onUpdate = viewModel.updater::downloadAndInstall
                )
            }
            if (!hasNotificationAccess) {
                AccessBanner(onOpenSettings = { openNotificationSettings(context) })
            }

            val track = uiState.currentTrack
            if (track == null) {
                NothingPlaying()
            } else {
                NowPlaying(track)
                LyricsSection(
                    lyrics = uiState.lyrics,
                    isLoading = uiState.isLoading,
                    hasError = uiState.error != null,
                    onOpenLyrics = onNavigateToLyrics,
                    onRetry = { viewModel.fetchLyrics(track) },
                    onOpenLibrary = onNavigateToLyricsManager
                )
            }
        }
    }
}

@Composable
private fun TopBar(onLibrary: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CarLyrics",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onLibrary) {
            Icon(Icons.Default.LibraryMusic, contentDescription = "Lyrics Library", tint = Color.White)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }
    }
}

@Composable
private fun NowPlaying(track: TrackInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (track.isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f))
            )
            Text(
                text = if (track.isPlaying) "Playing" else "Paused",
                style = MaterialTheme.typography.bodySmall,
                color = if (track.isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
            )
        }
        Text(
            text = track.track,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        track.album?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LyricsSection(
    lyrics: Lyrics?,
    isLoading: Boolean,
    hasError: Boolean,
    onOpenLyrics: () -> Unit,
    onRetry: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    when {
        isLoading -> PreviewSkeleton()
        lyrics != null -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onOpenLyrics,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (lyrics.isSynced) "Open synced lyrics" else "Open lyrics", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.sp)
            }
            Text(
                text = lyrics.plainLyrics.lines().filter { it.isNotBlank() }.take(PREVIEW_LINES).joinToString("\n"),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        else -> Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (hasError) "No lyrics found for this song" else "Lyrics not loaded yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            if (hasError) {
                Text(
                    text = "You can add them yourself by importing an .lrc file in the Lyrics Library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row {
                    TextButton(onClick = onRetry) { Text("Try again") }
                    TextButton(onClick = onOpenLibrary) { Text("Open Library") }
                }
            } else {
                TextButton(onClick = onRetry) { Text("Fetch lyrics") }
            }
        }
    }
}

@Composable
private fun NothingPlaying() {
    Column(modifier = Modifier.padding(top = 48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Nothing playing",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Start a song in Spotify. The lyrics follow along here, in your status bar, and on your car screen.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AccessBanner(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Turn on notification access", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(
            text = "CarLyrics needs it to see which song is playing.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open settings", fontWeight = FontWeight.Bold, letterSpacing = 0.sp) }
    }
}

@Composable
private fun PreviewSkeleton() {
    val alpha by rememberInfiniteTransition(label = "preview").animateFloat(
        initialValue = 0.08f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "previewAlpha"
    )
    Column(
        modifier = Modifier.semantics { contentDescription = "Loading lyrics" },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf(1f, 0.85f, 0.7f, 0.9f).forEach { width ->
            Box(
                Modifier
                    .fillMaxWidth(width)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}
