package com.mliem.carlyrics.di

import com.mliem.carlyrics.data.repository.LyricsRepositoryImpl
import com.mliem.carlyrics.data.repository.SettingsRepositoryImpl
import com.mliem.carlyrics.data.repository.TrackRepositoryImpl
import com.mliem.carlyrics.domain.repository.LyricsRepository
import com.mliem.carlyrics.domain.repository.SettingsRepository
import com.mliem.carlyrics.domain.repository.TrackRepository
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
