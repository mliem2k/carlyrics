package com.spotifylyrics.di

import android.content.Context
import androidx.room.Room
import com.spotifylyrics.data.local.database.LyricsDatabase
import com.spotifylyrics.data.local.database.dao.LyricsDao
import com.spotifylyrics.data.local.database.dao.TrackInfoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "lyrics_database"

    @Provides
    @Singleton
    fun provideLyricsDatabase(
        @ApplicationContext context: Context
    ): LyricsDatabase {
        return Room.databaseBuilder(
            context,
            LyricsDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLyricsDao(database: LyricsDatabase): LyricsDao {
        return database.lyricsDao()
    }

    @Provides
    @Singleton
    fun provideTrackInfoDao(database: LyricsDatabase): TrackInfoDao {
        return database.trackInfoDao()
    }
}
