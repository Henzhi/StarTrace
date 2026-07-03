package com.startrace.core.database.dao

import androidx.room.*
import com.startrace.core.database.entity.StoryFragmentRef
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryFragmentRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ref: StoryFragmentRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(refs: List<StoryFragmentRef>)

    @Delete
    suspend fun delete(ref: StoryFragmentRef)

    /** 删除某故事的所有碎片关联 */
    @Query("DELETE FROM story_fragment_refs WHERE story_id = :storyId")
    suspend fun deleteByStoryId(storyId: String)

    /** 删除某碎片的所有故事关联 */
    @Query("DELETE FROM story_fragment_refs WHERE fragment_id = :fragmentId")
    suspend fun deleteByFragmentId(fragmentId: String)

    /** 查询某故事关联的所有碎片 ID */
    @Query("SELECT fragment_id FROM story_fragment_refs WHERE story_id = :storyId")
    suspend fun getFragmentIdsByStoryId(storyId: String): List<String>

    /** 查询某碎片关联的所有故事 ID */
    @Query("SELECT story_id FROM story_fragment_refs WHERE fragment_id = :fragmentId")
    suspend fun getStoryIdsByFragmentId(fragmentId: String): List<String>

    /** 获取所有故事-碎片映射 (storyId → fragmentIds) */
    @Query("SELECT story_id, fragment_id FROM story_fragment_refs")
    fun observeAll(): Flow<List<StoryFragmentRef>>

    /** 批量获取故事-碎片映射 */
    @Query("""
        SELECT story_id, fragment_id FROM story_fragment_refs 
        WHERE fragment_id IN (:fragmentIds)
    """)
    suspend fun getByFragmentIds(fragmentIds: List<String>): List<StoryFragmentRef>

    /** 批量获取碎片-故事映射 */
    @Query("""
        SELECT story_id, fragment_id FROM story_fragment_refs 
        WHERE story_id IN (:storyIds)
    """)
    suspend fun getByStoryIds(storyIds: List<String>): List<StoryFragmentRef>
}
