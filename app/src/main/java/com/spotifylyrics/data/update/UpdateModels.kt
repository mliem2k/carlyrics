package com.mliem.carlyrics.data.update

import org.json.JSONArray

/** A release asset the app can install. [sha256] is null when GitHub does not report a digest. */
data class UpdateInfo(
    val tag: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val sha256: String?
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    /** Debug builds and sideloaded builds with a custom version name never self update. */
    data object Unsupported : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data object Installing : UpdateState
    data class Failed(val message: String) : UpdateState
}

// nightly_YYYYMMDD, or nightly_YYYYMMDD_N for the Nth build of that day (no suffix counts as 1).
private val NIGHTLY = Regex("""nightly_(\d{8})(?:_(\d+))?""")

private fun buildKey(name: String): Long? {
    val match = NIGHTLY.matchEntire(name) ?: return null
    val day = match.groupValues[1].toLong()
    val sequence = match.groupValues[2].ifEmpty { "1" }.toLong()
    return day * 1000 + sequence
}

/** True when [remoteTag] is a later nightly than [localVersionName]. */
fun isNewer(remoteTag: String, localVersionName: String): Boolean {
    val remote = buildKey(remoteTag) ?: return false
    val local = buildKey(localVersionName) ?: return false
    return remote > local
}

/** First published release (newest first) that carries an APK, from the GitHub releases list. */
fun parseLatestRelease(json: String): UpdateInfo? {
    val releases = JSONArray(json)
    for (i in 0 until releases.length()) {
        val release = releases.getJSONObject(i)
        if (release.optBoolean("draft", false)) continue
        val assets = release.optJSONArray("assets") ?: continue
        for (j in 0 until assets.length()) {
            val asset = assets.getJSONObject(j)
            if (!asset.optString("name").endsWith(".apk")) continue
            return UpdateInfo(
                tag = release.getString("tag_name"),
                apkUrl = asset.getString("browser_download_url"),
                sizeBytes = asset.optLong("size", -1L),
                sha256 = asset.optString("digest").removePrefix("sha256:").ifBlank { null }
            )
        }
    }
    return null
}
