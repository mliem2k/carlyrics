package com.spotifylyrics.data.repository

import com.spotifylyrics.data.local.preferences.SettingsPreferences
import com.spotifylyrics.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of SettingsRepository using DataStore
 */
class SettingsRepositoryImpl @Inject constructor(
    private val preferences: SettingsPreferences
) : SettingsRepository {

    companion object {
        private const val KEY_LRCLIB_ENABLED = "lrclib_enabled"
        private const val KEY_GENIUS_ENABLED = "genius_enabled"
        private const val KEY_MUSIXMATCH_ENABLED = "musixmatch_enabled"
        private const val KEY_PREFERRED_SOURCE = "preferred_source"
        private const val KEY_AUTO_FETCH = "auto_fetch"
        private const val KEY_CACHE_EXPIRY_DAYS = "cache_expiry_days"
    }

    override suspend fun setLrclibEnabled(enabled: Boolean) {
        preferences.setBoolean(KEY_LRCLIB_ENABLED, enabled)
    }

    override fun isLrclibEnabled(): Flow<Boolean> {
        return preferences.getBoolean(KEY_LRCLIB_ENABLED, true)
    }

    override suspend fun setGeniusEnabled(enabled: Boolean) {
        preferences.setBoolean(KEY_GENIUS_ENABLED, enabled)
    }

    override fun isGeniusEnabled(): Flow<Boolean> {
        return preferences.getBoolean(KEY_GENIUS_ENABLED, true)
    }

    override suspend fun setMusixmatchEnabled(enabled: Boolean) {
        preferences.setBoolean(KEY_MUSIXMATCH_ENABLED, enabled)
    }

    override fun isMusixmatchEnabled(): Flow<Boolean> {
        return preferences.getBoolean(KEY_MUSIXMATCH_ENABLED, true)
    }

    override suspend fun setPreferredSource(source: String) {
        preferences.setString(KEY_PREFERRED_SOURCE, source)
    }

    override fun getPreferredSource(): Flow<String> {
        return preferences.getString(KEY_PREFERRED_SOURCE, "genius")
    }

    override suspend fun setAutoFetch(enabled: Boolean) {
        preferences.setBoolean(KEY_AUTO_FETCH, enabled)
    }

    override fun isAutoFetchEnabled(): Flow<Boolean> {
        return preferences.getBoolean(KEY_AUTO_FETCH, true)
    }

    override suspend fun setCacheExpiryDays(days: Int) {
        preferences.setInt(KEY_CACHE_EXPIRY_DAYS, days)
    }

    override fun getCacheExpiryDays(): Flow<Int> {
        return preferences.getInt(KEY_CACHE_EXPIRY_DAYS, 7)
    }

    override fun getAllSettings(): Flow<Map<String, Boolean>> {
        return preferences.getAll().map { prefs ->
            mapOf(
                KEY_LRCLIB_ENABLED to (prefs[KEY_LRCLIB_ENABLED] as? Boolean ?: true),
                KEY_GENIUS_ENABLED to (prefs[KEY_GENIUS_ENABLED] as? Boolean ?: true),
                KEY_MUSIXMATCH_ENABLED to (prefs[KEY_MUSIXMATCH_ENABLED] as? Boolean ?: true),
                KEY_AUTO_FETCH to (prefs[KEY_AUTO_FETCH] as? Boolean ?: true)
            )
        }
    }
}
