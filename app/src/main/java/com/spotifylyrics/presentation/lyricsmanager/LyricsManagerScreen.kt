package com.mliem.carlyrics.presentation.lyricsmanager

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.carlyrics.domain.model.Lyrics

private val Panel = Color(0xFF1A1A1A)
private val Muted = Color.White.copy(alpha = 0.7f)

// Generous cap for a plain-text LRC file; guards against OutOfMemoryError if
// the user picks a large non-text file through the "*/*" picker.
private const val MAX_IMPORT_FILE_BYTES = 2 * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsManagerScreen(
    viewModel: LyricsManagerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingLrcContent by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Lyrics?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val buffer = CharArray(8192)
                val text = StringBuilder()
                var totalRead = 0
                while (true) {
                    val n = reader.read(buffer)
                    if (n < 0) break
                    totalRead += n
                    check(totalRead <= MAX_IMPORT_FILE_BYTES) { "File is too large to import" }
                    text.append(buffer, 0, n)
                }
                text.toString()
            } ?: ""
        }.onSuccess { content ->
            if (content.isNotBlank()) {
                pendingLrcContent = content
                showImportDialog = true
            }
        }
    }

    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Library", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.lyrics.isNotEmpty() || uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all saved lyrics", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import an .lrc file")
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            SearchField(uiState.searchQuery, viewModel::onSearchQueryChange)

            Spacer(Modifier.height(12.dp))

            if (uiState.lyrics.isEmpty()) {
                EmptyState(searching = uiState.searchQuery.isNotBlank(), query = uiState.searchQuery)
            } else {
                Text(
                    text = if (uiState.lyrics.size == 1) "1 saved song" else "${uiState.lyrics.size} saved songs",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
                LazyColumn {
                    items(uiState.lyrics, key = { "${it.artist}|${it.track}" }) { lyrics ->
                        SongRow(lyrics = lyrics, onClick = { selected = lyrics })
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }

    selected?.let { lyrics ->
        SongSheet(
            lyrics = lyrics,
            onDismiss = { selected = null },
            onExport = {
                viewModel.exportLrc(lyrics) { lrc ->
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "${lyrics.artist} ${lyrics.track}.lrc")
                        .putExtra(Intent.EXTRA_TEXT, lrc)
                    context.startActivity(Intent.createChooser(send, "Share lyrics"))
                }
            },
            onDelete = {
                viewModel.delete(lyrics)
                selected = null
            }
        )
    }

    if (showImportDialog) {
        ImportLrcDialog(
            lrcContent = pendingLrcContent,
            onConfirm = { track, artist ->
                viewModel.importLrc(track, artist, pendingLrcContent)
                showImportDialog = false
                pendingLrcContent = ""
            },
            onDismiss = {
                showImportDialog = false
                pendingLrcContent = ""
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Panel,
            title = { Text("Clear all saved lyrics?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes every saved song from this phone, including .lrc files you imported. Lyrics for songs you play are fetched again.",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearAllCache()
                }) { Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Color.White) } }
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search saved lyrics", color = Muted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Muted)
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Panel,
            unfocusedContainerColor = Panel,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun EmptyState(searching: Boolean, query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (searching) "No results" else "No saved lyrics yet",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = if (searching) "Nothing matches \"$query\"."
            else "Lyrics for the songs you play are saved here. Tap + to add your own from an .lrc file.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted
        )
    }
}

/** A song in the library: a colored letter tile (stable per song), its names, and a tag when it is synced. */
@Composable
private fun SongRow(lyrics: Lyrics, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LetterTile(seed = "${lyrics.artist}|${lyrics.track}", letter = lyrics.track.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪")
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = lyrics.track,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = lyrics.artist,
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (lyrics.isSynced) {
            Spacer(Modifier.width(12.dp))
            Text("Synced", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LetterTile(seed: String, letter: String) {
    // The same song always gets the same hue, so a list is easy to scan and nothing needs a cover image.
    val hue = seed.hashCode().mod(360).toFloat()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.hsv(hue, 0.45f, 0.34f)),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = Color.hsv(hue, 0.2f, 0.96f), fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

/**
 * Opens when a song is tapped: what you can do with it first (so the buttons are never pushed off
 * screen by a long song), then its lyrics to read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongSheet(lyrics: Lyrics, onDismiss: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(lyrics.track, color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(lyrics.artist, color = Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (lyrics.isSynced) "Synced lyrics" else "Plain lyrics, not time synced",
                    color = if (lyrics.isSynced) MaterialTheme.colorScheme.primary else Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Share .lrc", color = Color.White) }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) { Text("Delete") }
            }
            Text(
                text = lyrics.plainLyrics,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ImportLrcDialog(
    lrcContent: String,
    onConfirm: (track: String, artist: String) -> Unit,
    onDismiss: () -> Unit
) {
    var track by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Import .lrc file", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "File loaded (${lrcContent.lines().size} lines). Which song is it for?",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = track,
                    onValueChange = { track = it },
                    label = { Text("Song title") },
                    singleLine = true,
                    colors = dialogFieldColors()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    colors = dialogFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(track, artist) },
                enabled = track.isNotBlank() && artist.isNotBlank()
            ) {
                Text("Import", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Muted,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Muted,
    cursorColor = MaterialTheme.colorScheme.primary
)
