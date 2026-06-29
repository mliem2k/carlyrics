package com.mliem.carlyrics.data.remote.api

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * API service for fetching lyrics from lyrics.ovh API
 * This is a simple API that returns lyrics in JSON format
 * Note: Rate limited, use as fallback
 */
class LyricsOvhApiService(
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val BASE_URL = "https://api.lyrics.ovh/v1"
    }

    /**
     * Fetch lyrics for a track from lyrics.ovh
     */
    suspend fun fetchLyrics(trackInfo: TrackInfo): Lyrics? = withContext(Dispatchers.IO) {
        try {
            // Build API URL
            val artist = URLEncoder.encode(trackInfo.artist, StandardCharsets.UTF_8.toString())
            val track = URLEncoder.encode(trackInfo.track, StandardCharsets.UTF_8.toString())
            val url = "$BASE_URL/$artist/$track"

            // Make request
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null

            // Parse JSON response
            val json = JSONObject(responseBody)
            val lyricsText = json.optString("lyrics") ?: return@withContext null

            // Clean up lyrics
            val cleanLyrics = lyricsText
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("\\{.*?\\}"), "")
                .trim()

            Lyrics(
                track = trackInfo.track,
                artist = trackInfo.artist,
                album = trackInfo.album,
                plainLyrics = cleanLyrics,
                syncedLyrics = null
            )
        } catch (e: Exception) {
            null
        }
    }
}
