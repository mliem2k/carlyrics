package com.spotifylyrics.presentation.lyricsmanager

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.spotifylyrics.domain.model.Lyrics

private val SpotifyGreen = Color(0xFF1DB954)
private val DarkSurface = Color(0xFF282828)
private val DarkBg = Color(0xFF121212)
private val LightGray = Color(0xFFB3B3B3)

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
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
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
                title = { Text("Lyrics Library", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllCache() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = LightGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch("*/*") },
                containerColor = SpotifyGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import LRC")
            }
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search cached lyrics…", color = LightGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LightGray) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = LightGray)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = DarkSurface,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )

            Spacer(Modifier.height(12.dp))

            if (uiState.lyrics.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.searchQuery.isBlank()) "No cached lyrics yet.\nTap + to import an LRC file."
                               else "No results for \"${uiState.searchQuery}\"",
                        color = LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "${uiState.lyrics.size} cached",
                    color = LightGray,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.lyrics, key = { "${it.artist}|${it.track}" }) { lyrics ->
                        CachedLyricsCard(lyrics = lyrics)
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
private fun CachedLyricsCard(lyrics: Lyrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lyrics.track,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                Text(
                    text = lyrics.artist,
                    color = LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                if (lyrics.isSynced) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "● synced",
                        color = SpotifyGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${lyrics.plainLyrics.lines().size} lines",
                color = LightGray,
                style = MaterialTheme.typography.labelSmall
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
        containerColor = DarkSurface,
        title = { Text("Import LRC File", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "File loaded (${lrcContent.lines().size} lines). Enter the track details:",
                    color = LightGray,
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
                Text("Import", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LightGray)
            }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = SpotifyGreen,
    unfocusedBorderColor = LightGray,
    focusedLabelColor = SpotifyGreen,
    unfocusedLabelColor = LightGray,
    cursorColor = SpotifyGreen
)
