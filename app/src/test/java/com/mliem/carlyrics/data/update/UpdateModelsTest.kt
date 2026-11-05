package com.mliem.carlyrics.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateModelsTest {

    @Test
    fun buildNumberComesFromTheTag() {
        assertEquals(1781592056L, buildFromTag("nightly_20261105_1781592056"))
        assertNull(buildFromTag("nightly_20261105"))
        assertNull(buildFromTag("v3.0.0"))
        assertNull(buildFromTag("nightly_20261030_8"))
    }

    @Test
    fun higherBuildIsNewer() {
        assertTrue(isNewer("nightly_20261105_1781592200", installedBuild = 1781592100))
    }

    @Test
    fun sameOrLowerBuildIsNotNewer() {
        assertFalse(isNewer("nightly_20261105_1781592100", installedBuild = 1781592100))
        assertFalse(isNewer("nightly_20261105_1781592100", installedBuild = 1781592200))
    }

    @Test
    fun aSecondBuildOfTheSameDayIsNewer() {
        assertTrue(isNewer("nightly_20261105_1781592300", installedBuild = 1781592200))
    }

    @Test
    fun tagsWithoutABuildNumberNeverUpdate() {
        assertFalse(isNewer("nightly_20261105", installedBuild = 1))
        assertFalse(isNewer("nightly_20261030_8", installedBuild = 2))
        assertFalse(isNewer("v3.0.0", installedBuild = 1))
    }

    @Test
    fun picksTheHighestBuildEvenWhenTheListIsOutOfOrder() {
        val json = """[
            {"tag_name": "nightly_20261105_1781592150", "assets": [{"name": "mid.apk", "browser_download_url": "https://x/mid.apk"}]},
            {"tag_name": "nightly_20261106_1781592900", "draft": true, "assets": [{"name": "d.apk", "browser_download_url": "https://x/d.apk"}]},
            {"tag_name": "nightly_20261105_1781592400", "draft": false, "assets": [
                {"name": "notes.txt", "browser_download_url": "https://x/notes.txt"},
                {"name": "carlyrics_nightly_20261105.apk", "size": 4619645, "digest": "sha256:ABC123",
                 "browser_download_url": "https://x/carlyrics.apk"}]},
            {"tag_name": "nightly_20261029", "assets": [{"name": "old.apk", "browser_download_url": "https://x/old.apk"}]}
        ]"""

        assertEquals(UpdateInfo("nightly_20261105_1781592400", "https://x/carlyrics.apk", 4619645L, "ABC123"), parseLatestRelease(json))
    }

    @Test
    fun aReleaseWithoutAnApkIsSkipped() {
        val json = """[
            {"tag_name": "nightly_20261105_1781592500", "assets": []},
            {"tag_name": "nightly_20261105_1781592400", "assets": [{"name": "a.apk", "browser_download_url": "https://x/a.apk"}]}
        ]"""
        assertEquals("nightly_20261105_1781592400", parseLatestRelease(json)!!.tag)
    }

    @Test
    fun digestIsOptional() {
        val json = """[{"tag_name": "nightly_20261105_1781592400", "assets": [{"name": "a.apk", "size": 5, "browser_download_url": "https://x/a.apk"}]}]"""
        assertNull(parseLatestRelease(json)!!.sha256)
    }

    @Test
    fun noApkMeansNoUpdate() {
        assertNull(parseLatestRelease("""[{"tag_name": "nightly_20261105_1781592400", "assets": []}]"""))
        assertNull(parseLatestRelease("[]"))
    }
}
