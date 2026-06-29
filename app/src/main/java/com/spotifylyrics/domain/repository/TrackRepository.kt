package com.spotifylyrics.domain.repository

import com.spotifylyrics.domain.model.TrackInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for track operations
 */
interface TrackRepository {
    /**
     * Get current track info from notification listener
     */
    fun getCurrentTrackInfo(): Flow<TrackInfo?>

    /**
     * Get recent track history
     */
    fun getRecentTracks(limit: Int): Flow<List<TrackInfo>>

    /**
     * Save track to history
     */
    suspend fun saveTrackToHistory(trackInfo: TrackInfo)

    /**
     * Get track count in history
     */
    fun getTrackCount(): Flow<Int>

    /**
     * Clear track history
     */
    suspend fun clearHistory()

    /**
     * Get all unique artists from history
     */
    fun getAllArtists(): Flow<List<String>>
}
