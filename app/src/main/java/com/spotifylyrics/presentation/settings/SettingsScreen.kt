package com.mliem.carlyrics.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.carlyrics.BuildConfig
import com.mliem.carlyrics.presentation.update.UpdateCard

private val Panel = Color(0xFF1A1A1A)
private val Muted = Color.White.copy(alpha = 0.7f)
private const val SOURCE_URL = "https://github.com/mliem2k/carlyrics"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updater.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            SettingsSection(title = "Lyrics sources") {
                SwitchRow(
                    icon = Icons.Default.Lyrics,
                    title = "LRCLIB",
                    description = "Synced lyrics with timestamps. Tried first.",
                    checked = uiState.lrclibEnabled,
                    onCheckedChange = viewModel::toggleLrclib
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Default.TextSnippet,
                    title = "Genius",
                    description = "Plain lyrics if LRCLIB has none.",
                    checked = uiState.geniusEnabled,
                    onCheckedChange = viewModel::toggleGenius
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Default.TextSnippet,
                    title = "Musixmatch",
                    description = "Plain lyrics if the others have none.",
                    checked = uiState.musixmatchEnabled,
                    onCheckedChange = viewModel::toggleMusixmatch
                )
            }

            SettingsSection(title = "Playback") {
                SwitchRow(
                    icon = Icons.Default.Sync,
                    title = "Fetch lyrics automatically",
                    description = "Look them up as soon as a new song starts.",
                    checked = uiState.autoFetchEnabled,
                    onCheckedChange = viewModel::toggleAutoFetch
                )
            }

            SettingsSection(title = "App updates") {
                UpdateCard(
                    state = updateState,
                    currentVersion = BuildConfig.VERSION_NAME,
                    onCheck = viewModel.updater::check,
                    onUpdate = viewModel.updater::downloadAndInstall
                )
            }

            SettingsSection(title = "Storage") {
                ActionRow(
                    icon = Icons.Default.DeleteSweep,
                    title = "Clear saved lyrics",
                    description = savedSongsText(uiState.savedSongs),
                    enabled = uiState.savedSongs > 0,
                    onClick = { confirmClear = true }
                )
            }

            SettingsSection(title = "About") {
                ActionRow(
                    icon = Icons.Default.Code,
                    title = "Source code",
                    description = "github.com/mliem2k/carlyrics",
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) }
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Panel,
            title = { Text("Clear all saved lyrics?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${savedSongsText(uiState.savedSongs)} will be removed from this phone, including any .lrc files you imported. Lyrics for songs you play are fetched again.",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearCache()
                }) { Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Color.White) } }
        )
    }
}

private fun savedSongsText(count: Int) = when (count) {
    0 -> "Nothing saved yet"
    1 -> "1 saved song"
    else -> "$count saved songs"
}

/** A title and a rounded panel of rows. The panel groups the rows; dividers separate them. */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Muted,
            modifier = Modifier.padding(start = 8.dp)
        )
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel)) {
            content()
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = Color.White.copy(alpha = 0.08f))
}

@Composable
private fun LeadingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
    }
}

/** Icon, title and one line of help. The whole row toggles, so the target is as big as the row. */
@Composable
fun SwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingIcon(icon)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingIcon(icon)
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
            )
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}
