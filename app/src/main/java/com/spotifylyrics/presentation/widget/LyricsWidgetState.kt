package com.spotifylyrics.presentation.widget

data class LyricsWidgetState(
    val trackTitle: String = "",
    val artist: String = "",
    val currentLyricLine: String = "",
    val isPlaying: Boolean = false
)
