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

class AdvertisementTest {

    @Test
    fun detectsSpotifyAdBreaks() {
        assertEquals(true, looksLikeAdvertisement("Advertisement", "Spotify"))
        assertEquals(true, looksLikeAdvertisement("Spotify", "Advertisement"))
    }

    @Test
    fun leavesRealSongsAlone() {
        assertEquals(false, looksLikeAdvertisement("Blinding Lights", "The Weeknd"))
        assertEquals(false, looksLikeAdvertisement("Spotify", "Some Band"))
    }
}

class MusicPackagesTest {

    @Test
    fun spotifyAndYouTubeMusicAreFollowed() {
        assertEquals(true, "com.spotify.music" in MUSIC_PACKAGES)
        assertEquals(true, "com.google.android.apps.youtube.music" in MUSIC_PACKAGES)
    }

    @Test
    fun videoAppsAreIgnored() {
        assertEquals(false, "com.google.android.youtube" in MUSIC_PACKAGES)
        assertEquals(false, "com.netflix.mediaclient" in MUSIC_PACKAGES)
    }
}
