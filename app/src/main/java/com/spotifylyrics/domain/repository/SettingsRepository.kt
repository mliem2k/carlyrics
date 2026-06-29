package com.spotifylyrics.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings
 */
interface SettingsRepository {
    /**
     * Whether LRCLIB is enabled as lyrics source (synced lyrics)
     */
    suspend fun setLrclibEnabled(enabled: Boolean)
    fun isLrclibEnabled(): Flow<Boolean>

    /**
     * Whether Genius is enabled as lyrics source
     */
    suspend fun setGeniusEnabled(enabled: Boolean)
    fun isGeniusEnabled(): Flow<Boolean>

    /**
     * Whether Musixmatch is enabled as lyrics source
     */
    suspend fun setMusixmatchEnabled(enabled: Boolean)
    fun isMusixmatchEnabled(): Flow<Boolean>

    /**
     * Get preferred lyrics source order
     */
    suspend fun setPreferredSource(source: String)
    fun getPreferredSource(): Flow<String>

    /**
     * Whether to auto-fetch lyrics
     */
    suspend fun setAutoFetch(enabled: Boolean)
    fun isAutoFetchEnabled(): Flow<Boolean>

    /**
     * Get cache expiry duration in days
     */
    suspend fun setCacheExpiryDays(days: Int)
    fun getCacheExpiryDays(): Flow<Int>

    /**
     * Get all settings as a map
     */
    fun getAllSettings(): Flow<Map<String, Boolean>>
}
