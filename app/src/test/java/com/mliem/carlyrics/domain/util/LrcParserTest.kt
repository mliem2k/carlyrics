package com.mliem.carlyrics.domain.util

import com.mliem.carlyrics.domain.model.SyncedLyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `two digit centiseconds parse as hundredths of a second`() {
        val result = LrcParser.parse("[00:00.50]Is this the real life?")
        assertEquals(500L, result.single().startTime)
    }

    @Test
    fun `one digit tenths parse as tenths of a second`() {
        val result = LrcParser.parse("[00:01.5]half a second in")
        assertEquals(1500L, result.single().startTime)
    }

    @Test
    fun `three digit milliseconds parse as exact milliseconds`() {
        val result = LrcParser.parse("[00:01.234]word level timing")
        assertEquals(1234L, result.single().startTime)
    }

    @Test
    fun `an oversized digit run is skipped instead of crashing the whole parse`() {
        val result = LrcParser.parse(
            "[99999999999999999999:00.00]bad line\n[00:05.00]good line"
        )
        assertEquals(1, result.size)
        assertEquals("good line", result.single().text)
    }

    @Test
    fun `toLrcFormat output parses back to the same timestamps`() {
        val lines = listOf(
            SyncedLyricLine(500L, "line one"),
            SyncedLyricLine(2340L, "line two")
        )
        val reparsed = LrcParser.parse(LrcParser.toLrcFormat(lines))
        assertEquals(lines, reparsed)
    }

    @Test
    fun `isValidLrc is false for plain text with no time tags`() {
        assertTrue(!LrcParser.isValidLrc("just some plain lyrics\nwith no timestamps"))
    }
}
