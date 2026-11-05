package com.mliem.carlyrics.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:09", formatTime(9_999))
        assertEquals("1:23", formatTime(83_000))
        assertEquals("12:05", formatTime(725_000))
    }

    @Test
    fun formatsHoursForLongTracks() {
        assertEquals("1:01:05", formatTime(3_665_000))
    }

    @Test
    fun negativePositionsShowAsZero() {
        assertEquals("0:00", formatTime(-5_000))
    }
}
