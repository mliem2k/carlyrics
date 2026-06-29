package com.spotifylyrics.di

import com.spotifylyrics.data.repository.LyricsRepositoryImpl
import com.spotifylyrics.data.repository.SettingsRepositoryImpl
import com.spotifylyrics.data.repository.TrackRepositoryImpl
import com.spotifylyrics.domain.repository.LyricsRepository
import com.spotifylyrics.domain.repository.SettingsRepository
import com.spotifylyrics.domain.repository.TrackRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideLyricsRepository(
        impl: LyricsRepositoryImpl
    ): LyricsRepository = impl

    @Provides
    @Singleton
    fun provideTrackRepository(
        impl: TrackRepositoryImpl
    ): TrackRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository = impl
}
