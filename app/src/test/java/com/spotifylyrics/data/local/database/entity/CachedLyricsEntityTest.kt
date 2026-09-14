package com.mliem.carlyrics.data.local.database.entity

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.SyncedLyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedLyricsEntityTest {

    @Test
    fun `synced lyrics round trip through toEntity and toDomainModel`() {
        val original = Lyrics(
            track = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            plainLyrics = "Is this the real life?\nIs this just fantasy?",
            syncedLyrics = listOf(
                SyncedLyricLine(startTime = 500L, text = "Is this the real life?"),
                SyncedLyricLine(startTime = 2300L, text = "Is this just fantasy?")
            )
        )

        val restored = original.toEntity(source = "lrclib").toDomainModel()

        assertTrue("cached lyrics must still report as synced", restored.isSynced)
        assertEquals(original.syncedLyrics, restored.syncedLyrics)
    }

    @Test
    fun `plain lyrics with no synced lines round trip as unsynced`() {
        val original = Lyrics(
            track = "Track",
            artist = "Artist",
            plainLyrics = "some lyrics",
            syncedLyrics = null
        )

        val restored = original.toEntity(source = "genius").toDomainModel()

        assertTrue(restored.syncedLyrics.isNullOrEmpty())
        assertEquals(original.plainLyrics, restored.plainLyrics)
    }

    @Test
    fun `cache id does not collide across different track and artist pairs`() {
        // Under naive "${track}_$artist" concatenation these two pairs would
        // both produce "Foo_Bar_Baz" and silently evict each other's cache row.
        val idA = Lyrics(track = "Foo_Bar", artist = "Baz", plainLyrics = "").toEntity("x").id
        val idB = Lyrics(track = "Foo", artist = "Bar_Baz", plainLyrics = "").toEntity("x").id

        assertNotEquals(idA, idB)
    }
}
