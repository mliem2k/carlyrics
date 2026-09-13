package com.mliem.carlyrics.data.remote.api

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * API service for fetching lyrics from Genius.com
 * Uses web scraping with Jsoup
 */
class GeniusApiService(
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val BASE_URL = "https://genius.com"
        private const val SEARCH_URL = "$BASE_URL/search"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    /**
     * Fetch lyrics for a track from Genius
     */
    suspend fun fetchLyrics(trackInfo: TrackInfo): Lyrics? = withContext(Dispatchers.IO) {
        try {
            // Search for the song
            val searchQuery = "${trackInfo.artist} ${trackInfo.track}"
            val searchUrl = "$SEARCH_URL?q=${URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString())}"

            val doc = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .get()

            // Find the first search result
            val songLink = doc.selectFirst("a.song_link")
                ?: return@withContext null

            val songUrl = songLink.attr("href")
            if (!songUrl.startsWith("http")) {
                return@withContext null
            }

            // Fetch the song page
            val songDoc = Jsoup.connect(songUrl)
                .userAgent(USER_AGENT)
                .get()

            // Extract lyrics
            val lyricsContainer = songDoc.selectFirst(".lyrics")
                ?: songDoc.selectFirst("[data-lyrics-container='true']")

            val lyricsText = lyricsContainer?.text() ?: return@withContext null

            // Clean up lyrics
            val cleanLyrics = lyricsText
                .replace(Regex("\\[.*?\\]"), "") // Remove [Chorus], [Verse] etc.
                .replace(Regex("\\{.*?\\}"), "") // Remove {chorus}, {verse} etc.
                .trim()

            Lyrics(
                track = trackInfo.track,
                artist = trackInfo.artist,
                album = trackInfo.album,
                plainLyrics = cleanLyrics,
                syncedLyrics = null // Genius doesn't provide synced lyrics
            )
        } catch (e: Exception) {
            null
        }
    }
}
