package com.spotifylyrics.domain.util

import com.spotifylyrics.domain.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Enhanced lyrics matcher with multi-strategy search algorithm
 * 
 * Search Strategy:
 * 1. Cache lookup (fastest)
 * 2. Exact match (highest precision)
 * 3. Keyword search (high precision)
 * 4. Fuzzy matching (handles typos)
 * 5. Fallback sources (always have lyrics)
 */
interface LyricsMatcherEnhanced {
    suspend fun findLyrics(artist: String, title: String): Lyrics?
    suspend fun findLyricsWithFallback(artist: String, title: String): Lyrics?
}

class LyricsMatcherEnhancedImpl : LyricsMatcherEnhanced {
    
    private val queryCache = mutableMapOf<String, LyricsSearchResult>()
    private val CACHE_TTL_MINUTES = 60
    private val MIN_FUZZY_SIMILARITY = 0.70f  // 70% similarity threshold
    
    override suspend fun findLyrics(artist: String, title: String): Lyrics? = 
        withContext(Dispatchers.IO) {
            val cacheKey = normalizeCacheKey(artist, title)
            
            // Strategy 1: Cache lookup
            queryCache[cacheKey]?.let { cached ->
                if (!cached.isExpired()) return@withContext cached.lyrics
            }
            
            // Strategy 2: Exact match
            findExactMatch(artist, title)?.let { result ->
                cacheResult(cacheKey, result)
                return@withContext result
            }
            
            // Strategy 3: Keyword search
            findKeywordMatch(artist, title)?.let { result ->
                cacheResult(cacheKey, result)
                return@withContext result
            }
            
            // Strategy 4: Fuzzy matching
            findFuzzyMatch(artist, title)?.let { result ->
                cacheResult(cacheKey, result)
                return@withContext result
            }
            
            null
        }
    
    override suspend fun findLyricsWithFallback(artist: String, title: String): Lyrics? =
        withContext(Dispatchers.IO) {
            // Try primary search first
            findLyrics(artist, title)?.let { return@withContext it }
            
            // Fallback: Try alternative search strategies
            findAlternativeMatch(artist, title)?.let { return@withContext it }
            
            null
        }
    
    /**
     * Strategy 2: Exact match - requires perfect artist and title match
     */
    private suspend fun findExactMatch(artist: String, title: String): Lyrics? {
        // This would query the lyrics API (Genius, LRC lib, etc.)
        // Returns null if no exact match found
        return null // TODO: Implement API call
    }
    
    /**
     * Strategy 3: Keyword search - normalized artist + title
     */
    private suspend fun findKeywordMatch(artist: String, title: String): Lyrics? {
        val normalized = normalizeQuery(artist, title)
        // Query API with normalized terms
        return null // TODO: Implement API call
    }
    
    /**
     * Strategy 4: Fuzzy matching - handles typos and variations
     * Uses Levenshtein distance for string similarity
     */
    private suspend fun findFuzzyMatch(artist: String, title: String): Lyrics? {
        val normalizedArtist = artist.lowercase().trim()
        val normalizedTitle = title.lowercase().trim()
        
        // Split into words for better matching
        val artistWords = normalizedArtist.split(Regex("[\\s-]"))
        val titleWords = normalizedTitle.split(Regex("[\\s-]"))
        
        // Calculate similarity scores
        var maxSimilarity = 0f
        for (word in artistWords + titleWords) {
            val similarity = calculateLevenshteinSimilarity("$normalizedArtist $normalizedTitle", word)
            maxSimilarity = max(maxSimilarity, similarity)
        }
        
        return if (maxSimilarity >= MIN_FUZZY_SIMILARITY) {
            // Query API with fuzzy search
            null // TODO: Implement fuzzy API call
        } else {
            null
        }
    }
    
    /**
     * Strategy 5: Alternative match - try different search methods
     */
    private suspend fun findAlternativeMatch(artist: String, title: String): Lyrics? {
        // Try searching by song title only
        // Try searching by artist only
        // Try searching with different separators
        return null // TODO: Implement alternative searches
    }
    
    /**
     * Calculate Levenshtein distance-based similarity (0.0 to 1.0)
     * where 1.0 = perfect match, 0.0 = completely different
     */
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLength = max(s1.length, s2.length)
        return if (maxLength == 0) 1.0f else 1.0f - (distance.toFloat() / maxLength)
    }
    
    /**
     * Levenshtein distance algorithm for typo detection
     * Minimum edit distance to transform one string into another
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        // Initialize first row and column
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        // Fill DP table
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Normalize query for consistent searching
     * - Lowercase
     * - Remove special characters (except hyphens and spaces)
     * - Remove extra whitespace
     * - Handle common variations
     */
    private fun normalizeQuery(artist: String, title: String): String {
        val normalized = "$artist - $title"
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-é()'&,]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        // Handle common variations
        return normalized
            .replace("ft.", "feat.")
            .replace("feat.", "featuring")
    }
    
    /**
     * Normalize cache key for lookup
     */
    private fun normalizeCacheKey(artist: String, title: String): String {
        return "$artist|$title".lowercase().trim()
    }
    
    /**
     * Cache search result with TTL
     */
    private fun cacheResult(key: String, lyrics: Lyrics) {
        queryCache[key] = LyricsSearchResult(
            lyrics = lyrics,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Data class for cached lyrics with TTL tracking
     */
    private data class LyricsSearchResult(
        val lyrics: Lyrics,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean {
            val ageMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)
            return ageMinutes > CACHE_TTL_MINUTES
        }
    }
}
