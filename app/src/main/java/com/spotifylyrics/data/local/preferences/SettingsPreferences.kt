package com.spotifylyrics.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Interface for app settings preferences
 */
interface SettingsPreferences {
    suspend fun setBoolean(key: String, value: Boolean)
    suspend fun setString(key: String, value: String)
    suspend fun setInt(key: String, value: Int)
    fun getBoolean(key: String, default: Boolean): Flow<Boolean>
    fun getString(key: String, default: String): Flow<String>
    fun getInt(key: String, default: Int): Flow<Int>
    fun getAll(): Flow<Map<String, Any?>>
}

/**
 * Implementation of SettingsPreferences using DataStore
 */
class SettingsPreferencesImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsPreferences {

    override suspend fun setBoolean(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    override suspend fun setString(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun setInt(key: String, value: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey(key)] = value
        }
    }

    override fun getBoolean(key: String, default: Boolean): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: default
        }
    }

    override fun getString(key: String, default: String): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: default
        }
    }

    override fun getInt(key: String, default: Int): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[intPreferencesKey(key)] ?: default
        }
    }

    override fun getAll(): Flow<Map<String, Any?>> {
        return dataStore.data.map { preferences ->
            preferences.asMap().mapKeys { it.key.name }
        }
    }
}
