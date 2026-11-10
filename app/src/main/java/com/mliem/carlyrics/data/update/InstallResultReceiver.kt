package com.mliem.carlyrics.data.update

import android.content.Context
import com.mliem.android.update.InstallResultReceiver as CoreInstallResultReceiver
import com.mliem.android.update.UpdateClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives the system installer's result and hands it to the shared update client. */
@AndroidEntryPoint
class InstallResultReceiver : CoreInstallResultReceiver() {

    @Inject lateinit var updates: UpdateClient

    override fun updateClient(context: Context): UpdateClient = updates
}
