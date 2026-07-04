package com.startrace.core.database.dao

import androidx.room.*
import com.startrace.core.database.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Query("SELECT * FROM stories WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeAll(userId: String = ""): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE user_id = :userId AND style = :style ORDER BY created_at DESC")
    fun filterByStyle(style: String, userId: String = ""): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(story: StoryEntity)

    @Update
    suspend fun update(story: StoryEntity)

    @Delete
    suspend fun delete(story: StoryEntity)

    @Query("SELECT COUNT(*) FROM stories WHERE user_id = :userId")
    fun observeCount(userId: String = ""): Flow<Int>
}
