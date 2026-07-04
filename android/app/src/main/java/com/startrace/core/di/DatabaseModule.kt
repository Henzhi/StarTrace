package com.startrace.core.di

import com.startrace.core.database.AppDatabase
import com.startrace.core.database.MIGRATION_1_2
import com.startrace.core.database.MIGRATION_2_3
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
         .build()
    }

    @Provides @Singleton
    fun provideFragmentDao(db: AppDatabase) = db.fragmentDao()

    @Provides @Singleton
    fun provideStoryDao(db: AppDatabase) = db.storyDao()

    @Provides @Singleton
    fun provideLLMConfigDao(db: AppDatabase) = db.llmConfigDao()

    @Provides @Singleton
    fun provideStoryFragmentRefDao(db: AppDatabase) = db.storyFragmentRefDao()

    @Provides @Singleton
    fun provideUserDao(db: AppDatabase) = db.userDao()
}
