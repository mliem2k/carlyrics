package com.mliem.carlyrics.presentation.companion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mliem.carlyrics.domain.model.TrackInfo

private const val ANIM_MS = 220

// Dim levels keep large bold text above 3:1 against black.
private const val ALPHA_UPCOMING = 0.55f
private const val ALPHA_PAST = 0.38f

private val LyricStyle = TextStyle(
    fontSize = 28.sp,
    lineHeight = 34.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.3).sp
)
private val PlainLyricStyle = TextStyle(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)

/**
 * Full screen lyrics: the line being sung is bright, the rest are dimmed, and the list
 * follows playback until the user scrolls away. Tapping a line seeks the player to it.
 */
@Composable
fun CompanionLyricsScreen(
    viewModel: CompanionLyricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentIndex by viewModel.currentLyricIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isSynced = remember(uiState.lyrics) { uiState.lyrics.any { it.timeMs > 0 } }

    // Someone reading lyrics from a dash mount does not want the screen to time out.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TrackHeader(track = uiState.currentTrack, isSynced = isSynced, hasLyrics = uiState.lyrics.isNotEmpty())

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.isLoading -> LyricsSkeleton()
                uiState.lyrics.isNotEmpty() -> LyricsList(
                    lyrics = uiState.lyrics,
                    currentIndex = currentIndex,
                    isSynced = isSynced,
                    onSeek = viewModel::seekTo
                )
                uiState.error != null -> CenteredMessage(
                    title = "Lyrics unavailable",
                    detail = "Import an .lrc file from the Lyrics Library to add them yourself."
                )
                else -> CenteredMessage(
                    title = "Waiting for music",
                    detail = "Start a song in Spotify and the lyrics appear here."
                )
            }
        }

        PlaybackControls(
            isPlaying = isPlaying,
            onPlayPause = viewModel::togglePlayPause,
            onSkipPrevious = viewModel::skipPrevious,
            onSkipNext = viewModel::skipNext
        )
    }
}

@Composable
private fun TrackHeader(track: TrackInfo?, isSynced: Boolean, hasLyrics: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = track?.track ?: "Nothing playing",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        track?.let {
            Text(
                text = it.artist,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasLyrics) {
            Text(
                text = if (isSynced) "Synced lyrics" else "Lyrics are not time synced",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSynced) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private enum class LineState { Past, Current, Upcoming }

@Composable
private fun LyricsList(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    isSynced: Boolean,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    var following by remember { mutableStateOf(true) }

    // A finger on the list means the user wants to read ahead, so stop pulling it back.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) following = false }
    }
    LaunchedEffect(currentIndex, following, isSynced) {
        if (following && isSynced && currentIndex >= 0) listState.animateScrollToItem(currentIndex)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Top padding parks the current line in the upper third, where the eye rests.
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = maxHeight * 0.28f, bottom = maxHeight * 0.5f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                if (isSynced) {
                    val state = when {
                        index == currentIndex -> LineState.Current
                        index < currentIndex -> LineState.Past
                        else -> LineState.Upcoming
                    }
                    SyncedLine(
                        text = line.text,
                        state = state,
                        onClick = {
                            following = true
                            onSeek(line.timeMs)
                        }
                    )
                } else {
                    Text(
                        text = line.text,
                        style = PlainLyricStyle,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    )
                }
            }
        }

        if (!following && isSynced) {
            Surface(
                onClick = { following = true },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Sync to song",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.sp),
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SyncedLine(text: String, state: LineState, onClick: () -> Unit) {
    val targetAlpha = when (state) {
        LineState.Current -> 1f
        LineState.Upcoming -> ALPHA_UPCOMING
        LineState.Past -> ALPHA_PAST
    }
    val color by animateColorAsState(Color.White.copy(alpha = targetAlpha), tween(ANIM_MS, easing = FastOutSlowInEasing), label = "lineColor")
    // Scale instead of font size: the text never re-wraps while it changes emphasis.
    val scale by animateFloatAsState(if (state == LineState.Current) 1f else 0.9f, tween(ANIM_MS, easing = FastOutSlowInEasing), label = "lineScale")

    Text(
        text = text.ifBlank { "♪" },
        style = LyricStyle,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = state == LineState.Current }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}

@Composable
private fun LyricsSkeleton() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.08f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp).semantics { contentDescription = "Loading lyrics" },
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        listOf(0.9f, 0.7f, 0.8f, 0.55f, 0.85f, 0.6f).forEach { width ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun CenteredMessage(title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = Color.White, textAlign = TextAlign.Center)
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next track", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}
