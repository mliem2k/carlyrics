package com.spotifylyrics.domain.model

/**
 * Domain model representing track information
 */
data class TrackInfo(
    val track: String,
    val artist: String,
    val album: String? = null,
    val isPlaying: Boolean = false
) {
    /**
     * Generates unique key for this track
     */
    fun getKey(): String = "${track}_$artist"

    /**
     * Returns display title combining artist and track
     */
    fun getDisplayTitle(): String = "$artist - $track"

    companion object {
        /**
         * Empty track info placeholder
         */
        val Empty = TrackInfo(
            track = "",
            artist = "",
            album = null,
            isPlaying = false
        )
    }
}
