package com.startrace.core.database.dao

import androidx.room.*
import com.startrace.core.database.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Query("SELECT * FROM stories ORDER BY created_at DESC")
    fun observeAll(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: String): StoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(story: StoryEntity)

    @Update
    suspend fun update(story: StoryEntity)

    @Delete
    suspend fun delete(story: StoryEntity)

    @Query("SELECT COUNT(*) FROM stories")
    fun observeCount(): Flow<Int>
}
