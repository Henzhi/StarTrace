package com.startrace.core.database.dao

import androidx.room.*
import com.startrace.core.database.entity.FragmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FragmentDao {

    @Query("SELECT * FROM fragments WHERE is_archived = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FragmentEntity>>

    @Query("SELECT * FROM fragments WHERE id = :id")
    suspend fun getById(id: String): FragmentEntity?

    @Query("""
        SELECT * FROM fragments
        WHERE is_archived = 0
        AND (content LIKE '%' || :query || '%')
        ORDER BY created_at DESC
    """)
    fun search(query: String): Flow<List<FragmentEntity>>

    @Query("SELECT * FROM fragments WHERE is_archived = 0 AND domain_tag = :domain")
    fun filterByDomain(domain: String): Flow<List<FragmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fragment: FragmentEntity)

    @Query("UPDATE fragments SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Delete
    suspend fun delete(fragment: FragmentEntity)

    @Query("DELETE FROM fragments WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM fragments WHERE is_archived = 0")
    fun observeCount(): Flow<Int>
}
