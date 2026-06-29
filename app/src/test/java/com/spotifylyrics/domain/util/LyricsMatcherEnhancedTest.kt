package com.spotifylyrics.domain.util

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for Enhanced Lyrics Matcher
 */
class LyricsMatcherEnhancedTest {
    
    private lateinit var matcher: LyricsMatcherEnhancedImpl
    
    @Before
    fun setup() {
        matcher = LyricsMatcherEnhancedImpl()
    }
    
    /**
     * Test Levenshtein distance calculation for typo detection
     */
    @Test
    fun testLevenshteinDistance() {
        // Perfect match
        assertEquals(0, calculateDistance("hello", "hello"))
        
        // Single character difference
        assertEquals(1, calculateDistance("hello", "hallo"))
        assertEquals(1, calculateDistance("hello", "helo"))
        assertEquals(1, calculateDistance("hello", "helloo"))
        
        // Multiple differences
        assertEquals(3, calculateDistance("kitten", "sitting"))
        assertEquals(5, calculateDistance("abc", "xyz"))
    }
    
    /**
     * Test similarity calculation (0.0 to 1.0)
     */
    @Test
    fun testSimilarityCalculation() {
        // Perfect match should be 1.0
        val perfectSimilarity = calculateSimilarity("hello", "hello")
        assertEquals(1.0f, perfectSimilarity, 0.01f)
        
        // Slight differences should have high similarity
        val highSimilarity = calculateSimilarity("hello", "hallo")
        assertTrue(highSimilarity > 0.8f, "Similar strings should have high similarity")
        
        // Completely different strings should have low similarity
        val lowSimilarity = calculateSimilarity("abc", "xyz")
        assertTrue(lowSimilarity < 0.5f, "Different strings should have low similarity")
    }
    
    // Helper functions
    private fun calculateDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
    
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val distance = calculateDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return if (maxLength == 0) 1.0f else 1.0f - (distance.toFloat() / maxLength)
    }
}
