package com.spotifylyrics.presentation.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Phone Companion Lyrics Screen
 * 
 * Features:
 * - Real-time lyrics sync with playback
 * - Auto-scrolling to current lyric
 * - Night mode with brightness control
 * - Playback controls (play/pause, skip, seek)
 * - Responsive Material3 design
 */
@Composable
fun CompanionLyricsScreen(
    viewModel: CompanionLyricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()
    val isNightMode by viewModel.isNightMode.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val autoScrollEnabled by viewModel.autoScrollEnabled.collectAsState()
    
    val lazyListState = rememberLazyListState()
    
    // Auto-scroll to current lyric
    LaunchedEffect(currentLyricIndex, autoScrollEnabled) {
        if (autoScrollEnabled && uiState.lyrics.isNotEmpty()) {
            lazyListState.animateScrollToItem(
                index = maxOf(0, currentLyricIndex - 2),
                scrollOffset = -100
            )
        }
    }
    
    val backgroundColor = if (isNightMode) Color.Black else MaterialTheme.colorScheme.background
    val contentColor = if (isNightMode) Color.White else MaterialTheme.colorScheme.onBackground
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .alpha(brightness)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Track Info
            CurrentTrackHeader(
                track = uiState.currentTrack,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Main Content: Lyrics Display
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Error loading lyrics",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                uiState.lyrics.isNotEmpty() -> {
                    LyricsDisplay(
                        lyrics = uiState.lyrics,
                        currentIndex = currentLyricIndex,
                        lazyListState = lazyListState,
                        contentColor = contentColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No lyrics available",
                            color = contentColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Playback Controls
            PlaybackControlsBar(
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSkipNext = { viewModel.skipNext() },
                onToggleNightMode = { viewModel.toggleNightMode() },
                isNightMode = isNightMode,
                contentColor = contentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Header showing current track information
 */
@Composable
private fun CurrentTrackHeader(
    track: com.spotifylyrics.domain.model.TrackInfo?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        track?.let {
            Text(
                text = it.track,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it.artist,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } ?: run {
            Text(
                text = "No track playing",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Lyrics display with auto-scroll and highlighting
 */
@Composable
private fun LyricsDisplay(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 100.dp)
    ) {
        itemsIndexed(lyrics) { index, lyric ->
            val isCurrentLine = index == currentIndex
            
            Text(
                text = lyric.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isCurrentLine)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent
                    )
                    .padding(8.dp),
                style = if (isCurrentLine)
                    MaterialTheme.typography.headlineSmall
                else
                    MaterialTheme.typography.bodyLarge,
                color = if (isCurrentLine)
                    MaterialTheme.colorScheme.onPrimary
                else
                    contentColor,
                textAlign = TextAlign.Center,
                fontSize = if (isCurrentLine) 18.sp else 14.sp
            )
        }
    }
}

/**
 * Playback controls: play/pause, skip, night mode
 */
@Composable
private fun PlaybackControlsBar(
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleNightMode: () -> Unit,
    isNightMode: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Night mode toggle
        IconButton(
            onClick = onToggleNightMode,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isNightMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                contentDescription = "Toggle night mode",
                tint = contentColor
            )
        }
        
        // Skip previous
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous track",
                tint = contentColor
            )
        }
        
        // Play/Pause (larger)
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Skip next
        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next track",
                tint = contentColor
            )
        }
    }
}
