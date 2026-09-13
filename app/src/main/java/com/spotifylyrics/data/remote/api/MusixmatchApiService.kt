package com.mliem.carlyrics.data.remote.api

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * API service for fetching lyrics from Musixmatch
 * Uses web scraping with Jsoup
 */
class MusixmatchApiService(
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val BASE_URL = "https://www.musixmatch.com"
        private const val SEARCH_URL = "$BASE_URL/search"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    /**
     * Fetch lyrics for a track from Musixmatch
     */
    suspend fun fetchLyrics(trackInfo: TrackInfo): Lyrics? = withContext(Dispatchers.IO) {
        try {
            // Search for the song
            val searchQuery = "${trackInfo.artist} ${trackInfo.track}"
            val searchUrl = "$SEARCH_URL?q=${URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString())}"

            val doc = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            // Find the first search result
            val songLink = doc.selectFirst("a[href*=/lyrics/]")
                ?: return@withContext null

            val songUrl = songLink.attr("href")
            val fullUrl = if (songUrl.startsWith("http")) {
                songUrl
            } else {
                "$BASE_URL$songUrl"
            }

            // Fetch the song page
            val songDoc = Jsoup.connect(fullUrl)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            // Extract lyrics from the page
            val lyricsElement = songDoc.selectFirst("span.lyrics__content__ok")
                ?: songDoc.selectFirst(".lyrics-content")

            val lyricsText = lyricsElement?.text() ?: return@withContext null

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
