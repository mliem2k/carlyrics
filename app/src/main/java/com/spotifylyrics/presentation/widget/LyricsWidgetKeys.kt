package com.mliem.carlyrics.presentation.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object LyricsWidgetKeys {
    val TRACK_TITLE = stringPreferencesKey("widget_track_title")
    val ARTIST = stringPreferencesKey("widget_artist")
    val CURRENT_LYRIC = stringPreferencesKey("widget_current_lyric")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")
}
