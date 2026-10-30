package com.mliem.carlyrics.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.mliem.carlyrics.BuildConfig
import com.mliem.carlyrics.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Checks GitHub releases for a newer nightly, downloads it, and hands it to the system installer. */
@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val supported = !BuildConfig.DEBUG

    fun check() {
        if (!supported) {
            _state.value = UpdateState.Unsupported
            return
        }
        val current = _state.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading || current is UpdateState.Installing) return
        _state.value = UpdateState.Checking
        scope.launch {
            _state.value = try {
                val info = withContext(Dispatchers.IO) { fetchLatest() }
                if (info != null && isNewer(info.tag, BuildConfig.VERSION_NAME)) UpdateState.Available(info) else UpdateState.UpToDate
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                UpdateState.Failed("Could not check for updates. Check your connection and try again.")
            }
        }
    }

    fun downloadAndInstall() {
        val info = (_state.value as? UpdateState.Available)?.info ?: return
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.Failed("Allow CarLyrics to install apps in the settings screen that just opened, then tap Update again.")
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        _state.value = UpdateState.Downloading(0)
        scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) { download(info) }
                _state.value = UpdateState.Installing
                withContext(Dispatchers.IO) { install(apk) }
            } catch (e: Exception) {
                Log.w(TAG, "Update failed", e)
                _state.value = UpdateState.Failed(e.message ?: "The update failed.")
            }
        }
    }

    /** Called by [InstallResultReceiver] when the system installer rejects the APK. */
    fun onInstallFailed(message: String) {
        _state.value = UpdateState.Failed(message)
    }

    private fun fetchLatest(): UpdateInfo? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$REPO/releases?per_page=5")
            .header("Accept", "application/vnd.github+json")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "GitHub returned ${response.code}" }
            return parseLatestRelease(response.body!!.string())
        }
    }

    private fun download(info: UpdateInfo): File {
        val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
        val target = File(dir, "update.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        okHttpClient.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { response ->
            check(response.isSuccessful) { "Download failed (${response.code})" }
            val body = response.body!!
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
            var done = 0L
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) _state.value = UpdateState.Downloading((done * 100 / total).toInt())
                    }
                }
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (info.sha256 != null && !actual.equals(info.sha256, ignoreCase = true)) {
            target.delete()
            error("The downloaded file did not match its checksum, so it was not installed.")
        }
        return target
    }

    private fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("update.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            // The installer fills in the status extras, so this PendingIntent has to be mutable.
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(callback.intentSender)
        }
    }

    private companion object {
        const val TAG = "AppUpdater"
        const val REPO = "mliem2k/carlyrics"
    }
}
