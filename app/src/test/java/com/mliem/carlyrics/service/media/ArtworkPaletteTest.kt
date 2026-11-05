package com.mliem.carlyrics.service.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {

    private val blue = 0xFF2A6CF0.toInt()
    private val yellow = 0xFFFFD400.toInt()

    @Test
    fun emptyImageHasNoPalette() {
        assertNull(extractPalette(IntArray(0)))
    }

    @Test
    fun aSingleColorCoverUsesItTwice() {
        val palette = extractPalette(IntArray(1024) { blue })!!
        assertEquals(blue, palette.primary)
        assertEquals(blue, palette.secondary)
    }

    @Test
    fun theLargerAreaWinsAndTheOtherColorBecomesTheSecond() {
        val pixels = IntArray(1024) { if (it < 600) blue else yellow }
        val palette = extractPalette(pixels)!!
        assertEquals(blue, palette.primary)
        assertEquals(yellow, palette.secondary)
    }

    @Test
    fun aVividAccentBeatsASlightlyLargerGreyArea() {
        val grey = 0xFF808080.toInt()
        val pixels = IntArray(1000) { if (it < 520) grey else blue }
        assertEquals(blue, extractPalette(pixels)!!.primary)
    }

    @Test
    fun nearlyIdenticalShadesAreNotACandidateForTheSecondColor() {
        val closeToBlue = 0xFF2C6EF0.toInt()
        val pixels = IntArray(1024) { if (it < 600) blue else closeToBlue }
        val palette = extractPalette(pixels)!!
        assertEquals(palette.primary, palette.secondary)
    }

    @Test
    fun backdropColorsStayDarkEnoughForWhiteText() {
        for (color in listOf(yellow, blue, 0xFFFFFFFF.toInt(), 0xFFE8431C.toInt())) {
            val out = backdropColor(color)
            val brightest = maxOf((out shr 16) and 0xFF, (out shr 8) and 0xFF, out and 0xFF)
            assertTrue("brightest channel $brightest", brightest <= (0.42f * 255f + 1f).toInt())
        }
    }

    @Test
    fun backdropColorKeepsTheHue() {
        val out = backdropColor(blue)
        val r = (out shr 16) and 0xFF
        val b = out and 0xFF
        assertTrue("still blue", b > r)
        assertNotEquals(0xFF000000.toInt(), out)
    }

    @Test
    fun aDarkColorIsLeftAlone() {
        val dark = 0xFF202830.toInt()
        assertEquals(dark, backdropColor(dark))
    }
}
