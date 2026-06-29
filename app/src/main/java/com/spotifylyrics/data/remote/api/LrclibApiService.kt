package com.spotifylyrics.data.remote.api

import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.domain.util.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Client for lrclib.net — free, community-powered synced lyrics API.
 * Returns LRC-format timestamped lyrics (the key feature missing from other sources).
 * API docs: https://lrclib.net/docs
 */
class LrclibApiService(private val okHttpClient: OkHttpClient) {

    companion object {
        private const val BASE_URL = "https://lrclib.net/api"
        private const val USER_AGENT = "CarLyrics/1.0 (https://github.com/mliem2k/carlyrics)"
    }

    suspend fun fetchLyrics(trackInfo: TrackInfo): Lyrics? = withContext(Dispatchers.IO) {
        searchBestMatch(trackInfo.artist, trackInfo.track)
            ?.toLyrics(trackInfo)
    }

    private fun searchBestMatch(artist: String, track: String): JSONObject? {
        val results = search(artist, track) ?: return null
        if (results.length() == 0) return null

        // Prefer exact title+artist match; fall back to first result.
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            val matchesArtist = item.optString("artistName")
                .equals(artist, ignoreCase = true)
            val matchesTrack = item.optString("trackName")
                .equals(track, ignoreCase = true)
            if (matchesArtist && matchesTrack) return item
        }
        return results.getJSONObject(0)
    }

    private fun search(artist: String, track: String): JSONArray? {
        return try {
            val url = "$BASE_URL/search" +
                "?artist_name=${enc(artist)}" +
                "&track_name=${enc(track)}"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            JSONArray(body)
        } catch (e: Exception) {
            null
        }
    }

    private fun JSONObject.toLyrics(trackInfo: TrackInfo): Lyrics? {
        if (optBoolean("instrumental", false)) return null

        val plain = optString("plainLyrics").ifBlank { null } ?: return null
        val lrcString = optString("syncedLyrics").ifBlank { null }
        val synced = lrcString?.let { LrcParser.parse(it).ifEmpty { null } }

        return Lyrics(
            track = optString("trackName").ifBlank { trackInfo.track },
            artist = optString("artistName").ifBlank { trackInfo.artist },
            album = optString("albumName").ifBlank { trackInfo.album },
            plainLyrics = plain,
            syncedLyrics = synced
        )
    }

    private fun enc(value: String) =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
