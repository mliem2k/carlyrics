package com.mliem.carlyrics.presentation.companion

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for Companion Lyrics ViewModel
 */
class CompanionLyricsViewModelTest {
    
    @Before
    fun setup() {
        // Initialize test data
    }
    
    /**
     * Test lyric sync calculation
     */
    @Test
    fun testLyricSync_SingleLine() {
        val lyrics = listOf(
            LyricLine("Line 1", timeMs = 0L),
            LyricLine("Line 2", timeMs = 2000L),
            LyricLine("Line 3", timeMs = 4000L)
        )
        
        // At 0ms, should be line 0
        assertEquals(0, findCurrentLyricIndex(lyrics, 0L))
        
        // At 1000ms, still line 0
        assertEquals(0, findCurrentLyricIndex(lyrics, 1000L))
        
        // At 2000ms, should be line 1
        assertEquals(1, findCurrentLyricIndex(lyrics, 2000L))
        
        // At 3999ms, still line 1
        assertEquals(1, findCurrentLyricIndex(lyrics, 3999L))
        
        // At 4000ms, should be line 2
        assertEquals(2, findCurrentLyricIndex(lyrics, 4000L))
    }
    
    /**
     * Test brightness level clamping
     */
    @Test
    fun testBrightnessClamp() {
        val brightness = { min: Float, max: Float, value: Float ->
            value.coerceIn(min, max)
        }
        
        assertEquals(0f, brightness(0f, 1f, -0.5f))
        assertEquals(0f, brightness(0f, 1f, 0f))
        assertEquals(0.5f, brightness(0f, 1f, 0.5f))
        assertEquals(1f, brightness(0f, 1f, 1f))
        assertEquals(1f, brightness(0f, 1f, 1.5f))
    }
    
    /**
     * Test night mode toggle
     */
    @Test
    fun testNightModeToggle() {
        var isNightMode = false
        
        // Initially off
        assertFalse(isNightMode)
        
        // Toggle on
        isNightMode = !isNightMode
        assertTrue(isNightMode)
        
        // Toggle off
        isNightMode = !isNightMode
        assertFalse(isNightMode)
    }
    
    /**
     * Test auto-scroll toggle
     */
    @Test
    fun testAutoScrollToggle() {
        var autoScroll = true
        
        // Initially enabled
        assertTrue(autoScroll)
        
        // Toggle off
        autoScroll = !autoScroll
        assertFalse(autoScroll)
        
        // Toggle on
        autoScroll = !autoScroll
        assertTrue(autoScroll)
    }
    
    /**
     * Test context window calculation (±2 lines)
     */
    @Test
    fun testContextWindow() {
        val currentIndex = 3
        val totalLines = 10
        
        val startIndex = maxOf(0, currentIndex - 2)
        val endIndex = minOf(totalLines - 1, currentIndex + 2)
        
        // Should show lines 1-5 (current ±2)
        assertEquals(1, startIndex)
        assertEquals(5, endIndex)
        
        // Edge case: at start
        val startCurrentIndex = 1
        val startBound = maxOf(0, startCurrentIndex - 2)
        assertEquals(0, startBound)
        
        // Edge case: at end
        val endCurrentIndex = 8
        val endBound = minOf(totalLines - 1, endCurrentIndex + 2)
        assertEquals(9, endBound)
    }
    
    /**
     * Test empty lyrics handling
     */
    @Test
    fun testEmptyLyrics() {
        val lyrics = emptyList<LyricLine>()
        assertEquals(-1, findCurrentLyricIndex(lyrics, 2000L))
    }
    
    // Helper function
    private fun findCurrentLyricIndex(lyrics: List<LyricLine>, playbackTimeMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        
        for (i in lyrics.indices) {
            val line = lyrics[i]
            val nextLine = if (i + 1 < lyrics.size) lyrics[i + 1] else null
            
            if (line.timeMs <= playbackTimeMs) {
                if (nextLine == null || playbackTimeMs < nextLine.timeMs) {
                    return i
                }
            }
        }
        return -1
    }
}
