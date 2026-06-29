package com.spotifylyrics.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun update(
        trackTitle: String,
        artist: String,
        currentLyric: String,
        isPlaying: Boolean
    ) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(LyricsWidget::class.java)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[LyricsWidgetKeys.TRACK_TITLE] = trackTitle
                    this[LyricsWidgetKeys.ARTIST] = artist
                    this[LyricsWidgetKeys.CURRENT_LYRIC] = currentLyric
                    this[LyricsWidgetKeys.IS_PLAYING] = isPlaying
                }
            }
            LyricsWidget().update(context, glanceId)
        }
    }
}
