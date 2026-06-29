package com.spotifylyrics.data.remote.api

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LrclibApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: LrclibApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        // Point the service at the mock server by subclassing and overriding BASE_URL.
        // Since BASE_URL is a companion const, we use a wrapper that replaces the host.
        val client = OkHttpClient()
        service = object : LrclibApiService(client) {
            // Override not needed — we redirect via OkHttp interceptor below.
        }

        // Rebuild with a redirecting client so all requests go to MockWebServer.
        val redirectingClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val rewritten = original.newBuilder()
                    .url(
                        original.url.newBuilder()
                            .scheme(server.url("/").scheme)
                            .host(server.url("/").host)
                            .port(server.url("/").port)
                            .build()
                    )
                    .build()
                chain.proceed(rewritten)
            }
            .build()

        service = LrclibApiService(redirectingClient)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `returns synced lyrics when LRCLIB responds with syncedLyrics`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """[{
                        "id": 1,
                        "trackName": "Bohemian Rhapsody",
                        "artistName": "Queen",
                        "albumName": "A Night at the Opera",
                        "duration": 354.0,
                        "instrumental": false,
                        "plainLyrics": "Is this the real life?",
                        "syncedLyrics": "[00:00.50] Is this the real life?\n[00:02.30] Is this just fantasy?"
                    }]"""
                )
        )

        val result = service.fetchLyrics(
            com.spotifylyrics.domain.model.TrackInfo(track = "Bohemian Rhapsody", artist = "Queen")
        )

        assertNotNull(result)
        assertEquals("Bohemian Rhapsody", result!!.track)
        assertEquals("Queen", result.artist)
        assertTrue(result.isSynced)
        assertEquals(2, result.syncedLyrics!!.size)
        assertEquals(500L, result.syncedLyrics!![0].startTime)
        assertEquals("Is this the real life?", result.syncedLyrics!![0].text)
    }

    @Test
    fun `returns plain lyrics when syncedLyrics is null`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """[{
                        "id": 2,
                        "trackName": "Stairway to Heaven",
                        "artistName": "Led Zeppelin",
                        "albumName": "Led Zeppelin IV",
                        "duration": 482.0,
                        "instrumental": false,
                        "plainLyrics": "There's a lady who's sure",
                        "syncedLyrics": null
                    }]"""
                )
        )

        val result = service.fetchLyrics(
            com.spotifylyrics.domain.model.TrackInfo(track = "Stairway to Heaven", artist = "Led Zeppelin")
        )

        assertNotNull(result)
        assertTrue(result!!.plainLyrics.isNotEmpty())
        assertTrue(!result.isSynced)
    }

    @Test
    fun `returns null for instrumental tracks`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """[{
                        "id": 3,
                        "trackName": "Moonlight Sonata",
                        "artistName": "Beethoven",
                        "instrumental": true,
                        "plainLyrics": "",
                        "syncedLyrics": null
                    }]"""
                )
        )

        val result = service.fetchLyrics(
            com.spotifylyrics.domain.model.TrackInfo(track = "Moonlight Sonata", artist = "Beethoven")
        )

        assertNull(result)
    }

    @Test
    fun `returns null when server returns empty array`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = service.fetchLyrics(
            com.spotifylyrics.domain.model.TrackInfo(track = "Unknown Track", artist = "Unknown Artist")
        )

        assertNull(result)
    }

    @Test
    fun `returns null on HTTP error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = service.fetchLyrics(
            com.spotifylyrics.domain.model.TrackInfo(track = "Any Track", artist = "Any Artist")
        )

        assertNull(result)
    }

    @Test
    fun `prefers exact artist+title match over first result`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """[
                        {
                            "id": 10,
                            "trackName": "Hello (Live)",
                            "artistName": "Adele",
                            "instrumental": false,
                            "plainLyrics": "Live version",
                            "syncedLyrics": null
                        },
                        {
                            "id": 11,
                            "trackName": "Hello",
                            "artistName": "Adele",
                            "instrumental": false,
                            "plainLyrics": "Studio version",
                            "syncedLyrics": null
                        }
                    ]"""
                )
        )

        val result = service.fetchLyrics(
            com.spotifylyrics.domain.model.TrackInfo(track = "Hello", artist = "Adele")
        )

        assertNotNull(result)
        assertEquals("Studio version", result!!.plainLyrics)
    }
}
