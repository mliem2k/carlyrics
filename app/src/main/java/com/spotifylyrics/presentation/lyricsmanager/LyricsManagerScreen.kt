package com.mliem.carlyrics.presentation.lyricsmanager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
                title = { Text("Lyrics Library", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllCache() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = Muted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import LRC")
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search saved lyrics", color = Muted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Muted)
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
                    unfocusedContainerColor = Panel
                )
            )

            Spacer(Modifier.height(12.dp))

            if (uiState.lyrics.isEmpty()) {
                Column(modifier = Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (uiState.searchQuery.isBlank()) "No saved lyrics yet" else "No results",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = Color.White
                    )
                    Text(
                        text = if (uiState.searchQuery.isBlank()) "Lyrics you play are saved here. Tap + to add your own from an .lrc file."
                               else "Nothing matches \"${uiState.searchQuery}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )
                }
            } else {
                Text(
                    text = if (uiState.lyrics.size == 1) "1 saved song" else "${uiState.lyrics.size} saved songs",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(uiState.lyrics, key = { "${it.artist}|${it.track}" }) { lyrics ->
                        CachedLyricsRow(lyrics = lyrics)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
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
}

@Composable
private fun CachedLyricsRow(lyrics: Lyrics) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            Text(
                text = "Synced",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
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
        title = { Text("Import LRC File", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "File loaded (${lrcContent.lines().size} lines). Enter the track details:",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = track,
                    onValueChange = { track = it },
                    label = { Text("Track title") },
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
                Text("Cancel", color = Muted)
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
