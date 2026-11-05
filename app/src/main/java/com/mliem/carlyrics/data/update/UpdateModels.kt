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
    /** Debug builds never self update. */
    data object Unsupported : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data object Installing : UpdateState
    data class Failed(val message: String) : UpdateState
}

// nightly_YYYYMMDD_<build>, where <build> is the unix time the APK was built and the APK's versionCode.
// Earlier releases were tagged nightly_YYYYMMDD or nightly_YYYYMMDD_<n> with n the day's counter; a
// counter is far below any unix time, so those tags carry no build number.
private val NIGHTLY = Regex("""nightly_\d{8}_(\d+)""")
private const val MIN_UNIX_TIME = 1_000_000_000L

/** The build number a release tag carries, or null for a tag that is not a numbered nightly. */
fun buildFromTag(tag: String): Long? =
    NIGHTLY.matchEntire(tag)?.groupValues?.get(1)?.toLongOrNull()?.takeIf { it >= MIN_UNIX_TIME }

/** True when [remoteTag] carries a higher build number than the installed [installedBuild] (its versionCode). */
fun isNewer(remoteTag: String, installedBuild: Long): Boolean {
    val remote = buildFromTag(remoteTag) ?: return false
    return remote > installedBuild
}

/**
 * The release with the highest build number that carries an APK. The list is not trusted to be in
 * publish order: a deleted and recreated same-day release can sit anywhere in it.
 */
fun parseLatestRelease(json: String): UpdateInfo? {
    val releases = JSONArray(json)
    var best: UpdateInfo? = null
    var bestBuild = 0L
    for (i in 0 until releases.length()) {
        val release = releases.getJSONObject(i)
        if (release.optBoolean("draft", false)) continue
        val tag = release.optString("tag_name")
        val build = buildFromTag(tag) ?: continue
        if (build <= bestBuild) continue
        val assets = release.optJSONArray("assets") ?: continue
        for (j in 0 until assets.length()) {
            val asset = assets.getJSONObject(j)
            if (!asset.optString("name").endsWith(".apk")) continue
            bestBuild = build
            best = UpdateInfo(
                tag = tag,
                apkUrl = asset.getString("browser_download_url"),
                sizeBytes = asset.optLong("size", -1L),
                sha256 = asset.optString("digest").removePrefix("sha256:").ifBlank { null }
            )
            break
        }
    }
    return best
}
