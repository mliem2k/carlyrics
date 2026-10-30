package com.mliem.carlyrics.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateModelsTest {

    @Test
    fun laterNightlyIsNewer() {
        assertTrue(isNewer("nightly_20261105", "nightly_20261029"))
    }

    @Test
    fun sameOrOlderNightlyIsNotNewer() {
        assertFalse(isNewer("nightly_20261029", "nightly_20261029"))
        assertFalse(isNewer("nightly_20260913", "nightly_20261029"))
    }

    @Test
    fun laterBuildOfTheSameDayIsNewer() {
        assertTrue(isNewer("nightly_20261030_2", "nightly_20261030"))
        assertTrue(isNewer("nightly_20261030_3", "nightly_20261030_2"))
        assertFalse(isNewer("nightly_20261030_2", "nightly_20261030_2"))
        assertFalse(isNewer("nightly_20261030", "nightly_20261030_2"))
        assertFalse(isNewer("nightly_20261030_2", "nightly_20261031"))
    }

    @Test
    fun builtsThatAreNotNightliesNeverUpdate() {
        assertFalse(isNewer("nightly_20261105", "2.0.0-debug"))
        assertFalse(isNewer("v3.0.0", "nightly_20261029"))
    }

    @Test
    fun picksTheNewestPublishedReleaseWithAnApk() {
        val json = """[
            {"tag_name": "nightly_draft", "draft": true, "assets": [{"name": "a.apk", "browser_download_url": "https://x/a.apk"}]},
            {"tag_name": "nightly_20261105", "draft": false, "assets": [
                {"name": "notes.txt", "browser_download_url": "https://x/notes.txt"},
                {"name": "carlyrics_nightly_20261105.apk", "size": 4619645, "digest": "sha256:ABC123",
                 "browser_download_url": "https://x/carlyrics.apk"}]},
            {"tag_name": "nightly_20261029", "assets": [{"name": "old.apk", "browser_download_url": "https://x/old.apk"}]}
        ]"""

        val info = parseLatestRelease(json)

        assertEquals(UpdateInfo("nightly_20261105", "https://x/carlyrics.apk", 4619645L, "ABC123"), info)
    }

    @Test
    fun digestIsOptional() {
        val json = """[{"tag_name": "nightly_20261105", "assets": [{"name": "a.apk", "size": 5, "browser_download_url": "https://x/a.apk"}]}]"""
        assertNull(parseLatestRelease(json)!!.sha256)
    }

    @Test
    fun noApkMeansNoUpdate() {
        assertNull(parseLatestRelease("""[{"tag_name": "nightly_20261105", "assets": []}]"""))
        assertNull(parseLatestRelease("[]"))
    }
}
