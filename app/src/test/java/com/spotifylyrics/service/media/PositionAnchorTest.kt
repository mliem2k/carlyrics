package com.mliem.carlyrics.service.media

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionAnchorTest {

    @Test
    fun playingAdvancesFromTheReportedPosition() {
        val anchor = PositionAnchor(positionMs = 30_000, updatedAtMs = 1_000, speed = 1f, playing = true)
        assertEquals(32_500, anchor.estimateAt(3_500))
    }

    @Test
    fun pausedHoldsThePosition() {
        val anchor = PositionAnchor(positionMs = 30_000, updatedAtMs = 1_000, speed = 1f, playing = false)
        assertEquals(30_000, anchor.estimateAt(60_000))
    }

    @Test
    fun speedScalesElapsedTime() {
        val anchor = PositionAnchor(positionMs = 0, updatedAtMs = 0, speed = 2f, playing = true)
        assertEquals(2_000, anchor.estimateAt(1_000))
    }

    @Test
    fun unknownPositionStartsAtZero() {
        val anchor = PositionAnchor(positionMs = -1, updatedAtMs = 0, speed = 1f, playing = true)
        assertEquals(500, anchor.estimateAt(500))
    }

    @Test
    fun clockSkewNeverGoesBackwards() {
        val anchor = PositionAnchor(positionMs = 10_000, updatedAtMs = 5_000, speed = 1f, playing = true)
        assertEquals(10_000, anchor.estimateAt(4_000))
    }
}
