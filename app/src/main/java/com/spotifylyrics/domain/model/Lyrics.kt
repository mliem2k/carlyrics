package com.spotifylyrics.domain.model

/**
 * Domain model representing lyrics for a track
 */
data class Lyrics(
    val track: String,
    val artist: String,
    val album: String? = null,
    val plainLyrics: String,
    val syncedLyrics: List<SyncedLyricLine>? = null
) {
    /**
     * Checks if lyrics are synced
     */
    val isSynced: Boolean
        get() = !syncedLyrics.isNullOrEmpty()

    /**
     * Gets the current lyric line for a given playback position
     */
    fun getCurrentLyric(positionMs: Long): String? {
        return syncedLyrics
            ?.lastOrNull { it.startTime <= positionMs }
            ?.text
    }

    /**
     * Gets the index of the current lyric line for a given playback position
     */
    fun getCurrentLyricIndex(positionMs: Long): Int {
        return syncedLyrics
            ?.indexOfLast { it.startTime <= positionMs }
            ?: -1
    }

    companion object {
        /**
         * Empty lyrics placeholder
         */
        val Empty = Lyrics(
            track = "",
            artist = "",
            album = null,
            plainLyrics = ""
        )
    }
}

/**
 * Represents a single line of synced lyrics with timestamp
 */
data class SyncedLyricLine(
    val startTime: Long, // in milliseconds
    val text: String
)
