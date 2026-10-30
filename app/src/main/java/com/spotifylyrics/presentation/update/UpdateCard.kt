package com.mliem.carlyrics.presentation.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mliem.carlyrics.data.update.UpdateState

/** Update status with the one action that fits it. Shared by the home banner and Settings. */
@Composable
fun UpdateCard(
    state: UpdateState,
    currentVersion: String,
    onCheck: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title(state), style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(text = detail(state, currentVersion), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))

        when (state) {
            is UpdateState.Downloading -> LinearProgressIndicator(
                progress = { state.percent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            UpdateState.Checking, UpdateState.Installing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            is UpdateState.Available -> Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Update now") }
            UpdateState.Unsupported -> Unit
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCheck) { Text("Check for updates") }
            }
        }
    }
}

/** The home banner only appears when there is something to act on. */
fun UpdateState.needsAttention(): Boolean = this is UpdateState.Available ||
    this is UpdateState.Downloading || this is UpdateState.Installing || this is UpdateState.Failed

private fun title(state: UpdateState) = when (state) {
    is UpdateState.Available -> "Update available"
    is UpdateState.Downloading -> "Downloading update"
    UpdateState.Installing -> "Installing update"
    UpdateState.Checking -> "Checking for updates"
    UpdateState.UpToDate -> "You are up to date"
    UpdateState.Unsupported -> "Updates are off for this build"
    is UpdateState.Failed -> "Update problem"
    UpdateState.Idle -> "App updates"
}

private fun detail(state: UpdateState, currentVersion: String) = when (state) {
    is UpdateState.Available -> "${state.info.tag} is ready. You have $currentVersion."
    is UpdateState.Downloading -> "${state.percent}%"
    UpdateState.Installing -> "Follow the system prompt to finish."
    UpdateState.Unsupported -> "Debug builds are installed from the computer."
    is UpdateState.Failed -> state.message
    else -> "Installed: $currentVersion"
}
