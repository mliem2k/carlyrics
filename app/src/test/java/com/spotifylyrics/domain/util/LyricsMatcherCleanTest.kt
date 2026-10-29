package com.mliem.carlyrics.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsMatcherCleanTest {

    @Test
    fun stripsReleaseSuffixes() {
        assertEquals("Blinding Lights", LyricsMatcher.cleanTitle("Blinding Lights - Remastered 2011"))
        assertEquals("Yesterday", LyricsMatcher.cleanTitle("Yesterday - 2009 Remaster"))
        assertEquals("Song", LyricsMatcher.cleanTitle("Song - Radio Edit"))
        assertEquals("Song", LyricsMatcher.cleanTitle("Song (feat. Someone)"))
        assertEquals("Song", LyricsMatcher.cleanTitle("Song [Remastered]"))
    }

    @Test
    fun keepsRealTitles() {
        assertEquals("Live and Let Die", LyricsMatcher.cleanTitle("Live and Let Die"))
        assertEquals("Hey Ya! (Hey, hey, hey)", LyricsMatcher.cleanTitle("Hey Ya! (Hey, hey, hey)"))
        assertEquals("Remastered", LyricsMatcher.cleanTitle("Remastered"))
    }

    @Test
    fun primaryArtistTakesTheFirstCredit() {
        assertEquals("Post Malone", LyricsMatcher.primaryArtist("Post Malone, Swae Lee"))
        assertEquals("The Weeknd", LyricsMatcher.primaryArtist("The Weeknd"))
    }
}
