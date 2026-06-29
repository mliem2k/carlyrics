package com.spotifylyrics.di

import com.spotifylyrics.data.remote.api.GeniusApiService
import com.spotifylyrics.data.remote.api.LrclibApiService
import com.spotifylyrics.data.remote.api.LyricsOvhApiService
import com.spotifylyrics.data.remote.api.MusixmatchApiService
import com.spotifylyrics.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeniusApiService(
        okHttpClient: OkHttpClient
    ): GeniusApiService {
        return GeniusApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideMusixmatchApiService(
        okHttpClient: OkHttpClient
    ): MusixmatchApiService {
        return MusixmatchApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideLyricsOvhApiService(
        okHttpClient: OkHttpClient
    ): LyricsOvhApiService {
        return LyricsOvhApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideLrclibApiService(
        okHttpClient: OkHttpClient
    ): LrclibApiService {
        return LrclibApiService(okHttpClient)
    }
}
