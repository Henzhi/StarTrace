package com.startrace.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.startrace.core.database.dao.FragmentDao
import com.startrace.core.database.dao.LLMConfigDao
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.core.database.entity.LLMConfigEntity
import com.startrace.core.database.entity.StoryEntity

@Database(
    entities = [
        FragmentEntity::class,
        StoryEntity::class,
        LLMConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fragmentDao(): FragmentDao
    abstract fun storyDao(): StoryDao
    abstract fun llmConfigDao(): LLMConfigDao
}
