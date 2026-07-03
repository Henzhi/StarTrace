package com.startrace.core.database.dao

import androidx.room.*
import com.startrace.core.database.entity.LLMConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LLMConfigDao {

    @Query("SELECT * FROM llm_configs ORDER BY created_at DESC")
    fun observeAll(): Flow<List<LLMConfigEntity>>

    @Query("SELECT * FROM llm_configs WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): LLMConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: LLMConfigEntity)

    @Query("UPDATE llm_configs SET is_default = 0")
    suspend fun clearDefault()

    @Delete
    suspend fun delete(config: LLMConfigEntity)
}
