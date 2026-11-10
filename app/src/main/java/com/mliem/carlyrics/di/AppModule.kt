package com.mliem.carlyrics.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mliem.android.update.Channel
import com.mliem.android.update.UpdateClient
import com.mliem.android.update.UpdateConfig
import com.mliem.android.update.UpdateMessages
import com.mliem.carlyrics.BuildConfig
import com.mliem.carlyrics.data.local.preferences.SettingsPreferences
import com.mliem.carlyrics.data.local.preferences.SettingsPreferencesImpl
import com.mliem.carlyrics.data.update.InstallResultReceiver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideSettingsPreferences(dataStore: DataStore<Preferences>): SettingsPreferences {
        return SettingsPreferencesImpl(dataStore)
    }

    @Provides
    @ApplicationScope
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideUpdateClient(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        @ApplicationScope scope: CoroutineScope,
    ): UpdateClient = UpdateClient(
        context = context,
        config = UpdateConfig(
            repo = "mliem2k/carlyrics",
            installReceiver = InstallResultReceiver::class.java,
            channels = listOf(Channel.NIGHTLY),
            updatesEnabled = !BuildConfig.DEBUG,
            messages = UpdateMessages(
                permission = "Allow CarLyrics to install apps in the settings screen that just opened, then tap Update again.",
                signature = "This build is signed with a different key than the installed app. Uninstall CarLyrics once, then install the new APK.",
            ),
        ),
        http = okHttpClient,
        scope = scope,
    )
}
