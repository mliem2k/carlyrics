package com.mliem.carlyrics.presentation.main

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mliem.carlyrics.BuildConfig
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.presentation.companion.CompanionLyricsViewModel
import com.mliem.carlyrics.presentation.companion.LyricLine
import com.mliem.carlyrics.presentation.update.UpdateCard
import com.mliem.carlyrics.service.media.ArtworkPalette
import com.mliem.carlyrics.service.media.QueueEntry
import com.mliem.carlyrics.service.media.backdropColor
import com.mliem.carlyrics.presentation.update.needsAttention
import kotlinx.coroutines.flow.StateFlow

private const val ANIM_MS = 220

private const val GLOW_ALPHA = 0.8f

// Dim levels keep large bold text above 3:1 against black.
private const val ALPHA_UPCOMING = 0.55f
private const val ALPHA_PAST = 0.38f

private val Panel = Color(0xFF1A1A1A)
private val Muted = Color.White.copy(alpha = 0.7f)

private val LyricStyle = TextStyle(
    fontSize = 28.sp,
    lineHeight = 34.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.3).sp
)
private val PlainLyricStyle = TextStyle(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)

/**
 * The one screen the app lives on: what is playing, its lyrics following the song, and the
 * player controls. Library and Settings are one tap away in the top bar.
 */
@Composable
fun MainScreen(
    viewModel: CompanionLyricsViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLyricsManager: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentIndex by viewModel.currentLyricIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val updateState by viewModel.updater.state.collectAsState()
    val isSynced = remember(uiState.lyrics) { uiState.lyrics.any { it.timeMs > 0 } }
    val context = LocalContext.current

    var hasNotificationAccess by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    // The grant happens in system settings, so re-check on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationAccess = isNotificationListenerEnabled(context)
    }

    // Someone reading lyrics from a dash mount does not want the screen to time out.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val track = uiState.currentTrack
    val queue by viewModel.queue.collectAsState()
    val activeQueueId by viewModel.activeQueueId.collectAsState()
    var showQueue by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    ArtworkBackdrop(viewModel.artworkPalette)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopBar(
            track = track,
            onLibrary = onNavigateToLyricsManager,
            onSettings = onNavigateToSettings
        )

        if (updateState.needsAttention() || !hasNotificationAccess) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                track == null -> Message(
                    title = "Nothing playing",
                    detail = "Start a song in Spotify. The lyrics follow along here, in your status bar, and on your car screen."
                )
                uiState.isLoading -> LyricsSkeleton()
                uiState.lyrics.isNotEmpty() -> LyricsList(
                    lyrics = uiState.lyrics,
                    currentIndex = currentIndex,
                    isSynced = isSynced,
                    onSeek = viewModel::seekTo
                )
                uiState.error != null -> Message(
                    title = "No lyrics found for this song",
                    detail = "You can add them yourself by importing an .lrc file in the Lyrics Library.",
                    actions = {
                        TextButton(onClick = viewModel::retryLyrics) { Text("Try again") }
                        TextButton(onClick = onNavigateToLyricsManager) { Text("Open Library") }
                    }
                )
                else -> Message(
                    title = "Lyrics not loaded yet",
                    detail = "Automatic lookup is off in Settings.",
                    actions = { TextButton(onClick = viewModel::retryLyrics) { Text("Fetch lyrics") } }
                )
            }
        }

        if (track != null) {
            SeekBar(
                positionMs = viewModel.playbackPositionMs,
                durationMs = viewModel.durationMs,
                onSeek = viewModel::seekTo
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                PlaybackControls(
                    isPlaying = isPlaying,
                    onPlayPause = viewModel::togglePlayPause,
                    onSkipPrevious = viewModel::skipPrevious,
                    onSkipNext = viewModel::skipNext
                )
                // Only players that publish a queue get the button.
                if (queue.size > 1) {
                    IconButton(
                        onClick = { showQueue = true },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = Color.White)
                    }
                }
            }
        }
    }
    }

    if (showQueue) {
        QueueSheet(
            queue = queue,
            activeId = activeQueueId,
            onSelect = {
                viewModel.skipToQueueItem(it)
                showQueue = false
            },
            onDismiss = { showQueue = false }
        )
    }
}

/**
 * A smooth gradient painted from the cover's colors: the main color fading to black from the top, with a
 * soft glow of the second color in the corner. Colors, not the picture, so there is no resolution to run
 * out of and nothing to pixelate, and the colors are pre-darkened so the lyrics stay readable.
 */
@Composable
private fun ArtworkBackdrop(palette: StateFlow<ArtworkPalette?>) {
    val colors by palette.collectAsState()
    val spec = tween<Color>(durationMillis = 600)
    val base by animateColorAsState(colors?.let { Color(backdropColor(it.primary)) } ?: Color.Black, spec, label = "backdropBase")
    val glow by animateColorAsState(colors?.let { Color(backdropColor(it.secondary)) } ?: Color.Black, spec, label = "backdropGlow")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(base, Color.Black)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = GLOW_ALPHA), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.12f),
                        radius = size.maxDimension * 0.6f
                    )
                )
            }
    )
}

/** The player's queue. The playing song is marked; tapping another one skips to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(queue: List<QueueEntry>, activeId: Long, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Text(
            text = "Queue",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.navigationBarsPadding()) {
            items(queue, key = { it.id }) { entry ->
                val playing = entry.id == activeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(entry.id) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (playing) MaterialTheme.colorScheme.primary else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        entry.subtitle?.takeIf { it.isNotBlank() }?.let {
                            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (playing) {
                        Text("Playing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    track: TrackInfo?,
    onLibrary: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track?.track ?: "CarLyrics",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            track?.let {
                Text(
                    text = it.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onLibrary) {
            Icon(Icons.Default.LibraryMusic, contentDescription = "Lyrics Library", tint = Color.White)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
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
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = maxHeight * 0.22f, bottom = maxHeight * 0.5f),
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
                    style = MaterialTheme.typography.labelLarge,
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
private fun Message(title: String, detail: String, actions: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
            color = Muted
        )
        Row { actions() }
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
            color = Muted
        )
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open settings", fontWeight = FontWeight.Bold) }
    }
}

/**
 * Progress through the song with elapsed and total time. Collecting the position here keeps its
 * quarter second updates from redrawing the whole screen. Hidden when the player reports no length.
 */
@Composable
private fun SeekBar(
    positionMs: StateFlow<Long>,
    durationMs: StateFlow<Long>,
    onSeek: (Long) -> Unit
) {
    val position by positionMs.collectAsState()
    val duration by durationMs.collectAsState()
    if (duration <= 0L) return

    // While a finger is on the bar it shows the finger, not the player.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: (position.toFloat() / duration).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * duration).toLong()) }
                dragFraction = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.semantics { contentDescription = "Song position" }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((fraction * duration).toLong()), style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
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
