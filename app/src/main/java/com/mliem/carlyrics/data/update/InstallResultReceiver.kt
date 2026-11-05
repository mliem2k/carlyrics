package com.mliem.carlyrics.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives the system installer's result: shows its confirmation screen, or reports why it refused. */
@AndroidEntryPoint
class InstallResultReceiver : BroadcastReceiver() {

    @Inject lateinit var updater: AppUpdater

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(context::startActivity)
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> updater.onInstallFailed(
                "This build is signed with a different key than the installed app. Uninstall CarLyrics once, then install the new APK."
            )
            else -> updater.onInstallFailed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "The installer could not install the update."
            )
        }
    }
}
