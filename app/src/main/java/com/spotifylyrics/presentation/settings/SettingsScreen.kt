package com.spotifylyrics.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotifylyrics.presentation.theme.SpotifyGreen
import com.spotifylyrics.presentation.theme.SpotifyLightGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
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
                .padding(16.dp)
        ) {
            SettingsSection(title = "Lyrics Sources") {
                SwitchSetting(
                    title = "LRCLIB (synced)",
                    description = "Free synced lyrics, tried first for timestamped lines",
                    checked = uiState.lrclibEnabled,
                    onCheckedChange = { viewModel.toggleLrclib(it) }
                )
                SwitchSetting(
                    title = "Genius",
                    description = "Plain lyrics fallback via Genius.com",
                    checked = uiState.geniusEnabled,
                    onCheckedChange = { viewModel.toggleGenius(it) }
                )
                SwitchSetting(
                    title = "Musixmatch",
                    description = "Plain lyrics fallback via Musixmatch",
                    checked = uiState.musixmatchEnabled,
                    onCheckedChange = { viewModel.toggleMusixmatch(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Cache") {
                SwitchSetting(
                    title = "Auto-fetch Lyrics",
                    description = "Automatically fetch lyrics when track changes",
                    checked = uiState.autoFetchEnabled,
                    onCheckedChange = { viewModel.toggleAutoFetch(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.clearCache() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Cache", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SpotifyLightGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SpotifyLightGray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SpotifyGreen,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}
