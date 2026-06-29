package com.mliem.carlyrics.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mliem.carlyrics.presentation.main.MainActivity

class LyricsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                LyricsWidgetContent()
            }
        }
    }
}

@Composable
private fun LyricsWidgetContent() {
    val prefs = currentState<androidx.datastore.preferences.core.Preferences>()

    val trackTitle = prefs[LyricsWidgetKeys.TRACK_TITLE] ?: ""
    val artist = prefs[LyricsWidgetKeys.ARTIST] ?: ""
    val lyricLine = prefs[LyricsWidgetKeys.CURRENT_LYRIC] ?: ""
    val isPlaying = prefs[LyricsWidgetKeys.IS_PLAYING] ?: false

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1A1A2E)))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        if (trackTitle.isEmpty() && lyricLine.isEmpty()) {
            Text(
                text = "CarLyrics",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF888888)),
                    fontSize = 14.sp
                )
            )
            Text(
                text = "Play music to see lyrics",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF555555)),
                    fontSize = 12.sp
                )
            )
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = if (isPlaying) "▶" else "⏸",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF1DB954)),
                        fontSize = 10.sp
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
            }

            Spacer(GlanceModifier.height(4.dp))

            Text(
                text = trackTitle,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Text(
                text = artist,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFAAAAAA)),
                    fontSize = 11.sp
                ),
                maxLines = 1
            )

            Spacer(GlanceModifier.height(8.dp))

            Text(
                text = lyricLine.ifEmpty { "♪" },
                style = TextStyle(
                    color = ColorProvider(Color(0xFF1DB954)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2
            )
        }
    }
}
