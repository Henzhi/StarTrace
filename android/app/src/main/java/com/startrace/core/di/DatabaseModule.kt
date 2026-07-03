package com.startrace.core.di

import com.startrace.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "startrace.db"
        ).build()
    }

    @Provides @Singleton
    fun provideFragmentDao(db: AppDatabase) = db.fragmentDao()

    @Provides @Singleton
    fun provideStoryDao(db: AppDatabase) = db.storyDao()

    @Provides @Singleton
    fun provideLLMConfigDao(db: AppDatabase) = db.llmConfigDao()
}
