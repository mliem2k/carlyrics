package com.mliem.carlyrics.service.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ArtworkPaletteLoaderTest {

    @Test
    fun slowAThenFastBPublishesOnlyB() = runTest {
        val computed = mutableListOf<String>()
        val completed = mutableListOf<String>()
        val delivered = mutableListOf<ArtworkPalette>()
        val job = LatestArtworkJob<String>(this) { input ->
            computed += input
            if (input == "a") delay(1_000)
            completed += input
            paletteOf(input)
        }

        job.submit("a", "a") { delivered += it!! }
        runCurrent()
        job.submit("b", "b") { delivered += it!! }
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), computed)
        // B cancels A, so A never finishes its work.
        assertEquals(listOf("b"), completed)
        assertEquals(listOf(paletteOf("b")), delivered)
    }

    @Test
    fun sameKeyConcurrentSubmissionsComputeOnce() = runTest {
        var computations = 0
        val delivered = mutableListOf<ArtworkPalette>()
        val job = LatestArtworkJob<String>(this) { input ->
            computations++
            delay(1_000)
            paletteOf(input)
        }

        job.submit("a", "a") { delivered += it!! }
        runCurrent() // the first computation is in flight
        job.submit("a", "a") { delivered += it!! }
        advanceUntilIdle()

        assertEquals(1, computations)
        assertEquals(listOf(paletteOf("a")), delivered)
    }

    @Test
    fun aResultWhoseKeyIsNoLongerCurrentIsDropped() = runTest {
        val delivered = mutableListOf<ArtworkPalette>()
        val job = LatestArtworkJob<String>(this) { input ->
            if (input == "a") {
                // Finishes after B took over: only the key check can stop this result.
                try {
                    delay(1_000)
                } catch (_: CancellationException) {
                }
            }
            paletteOf(input)
        }

        job.submit("a", "a") { delivered += it!! }
        runCurrent()
        job.submit("b", "b") { delivered += it!! }
        advanceUntilIdle()

        assertEquals(listOf(paletteOf("b")), delivered)
    }

    @Test
    fun clearCancelsAndSuppressesResults() = runTest {
        var cancelled = false
        var finished = false
        val delivered = mutableListOf<ArtworkPalette>()
        val job = LatestArtworkJob<String>(this) { input ->
            try {
                delay(1_000)
            } catch (_: CancellationException) {
                cancelled = true
            }
            // Even a computation that ignores cancellation must not publish after clear.
            finished = true
            paletteOf(input)
        }

        job.submit("a", "a") { delivered += it!! }
        runCurrent()
        job.clear()
        advanceUntilIdle()

        assertTrue(cancelled)
        assertTrue(finished)
        assertTrue(delivered.isEmpty())
    }

    @Test
    fun theOldJobUnwindingDoesNotClearTheNewActiveKey() = runTest {
        val computed = mutableListOf<String>()
        val job = LatestArtworkJob<String>(this) { input ->
            computed += input
            delay(1_000)
            paletteOf(input)
        }

        job.submit("a", "a") {}
        runCurrent() // A is in flight
        job.submit("b", "b") {}
        runCurrent() // A unwinds after its cancellation, B starts
        job.submit("b", "b") {} // still a duplicate of the in-flight B
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), computed)
    }

    @Test
    fun computeRunsOnTheInjectedDispatcher() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var computeDispatcher: CoroutineDispatcher? = null
        val job = LatestArtworkJob<String>(CoroutineScope(dispatcher)) { input ->
            computeDispatcher =
                currentCoroutineContext()[ContinuationInterceptor] as CoroutineDispatcher
            paletteOf(input)
        }

        job.submit("a", "a") {}
        assertNull(computeDispatcher) // not computed inline on the caller
        runCurrent()
        assertSame(dispatcher, computeDispatcher)
        assertNotSame(currentCoroutineContext()[ContinuationInterceptor], computeDispatcher)
    }

    private fun paletteOf(input: String): ArtworkPalette {
        val color = input.hashCode()
        return ArtworkPalette(color, color)
    }
}
