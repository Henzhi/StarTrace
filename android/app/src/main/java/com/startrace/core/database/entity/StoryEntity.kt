package com.startrace.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 生成故事实体
 */
@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String = "",         // 关联用户，"" 表示未登录

    val title: String,
    val content: String,

    @ColumnInfo(name = "fragment_ids_json")
    val fragmentIdsJson: String,    // JSON Array of fragment IDs

    val length: String,             // short/medium/long
    val style: String,              // scifi/fantasy/realistic/prose/poetry/mystery

    val positionX: Float = 0f,
    val positionY: Float = 0f,

    @ColumnInfo(name = "is_public")
    val isPublic: Boolean = false,

    @ColumnInfo(name = "public_id")
    val publicId: String? = null,    // 服务器上传后赋值

    @ColumnInfo(name = "llm_config_id")
    val llmConfigId: String? = null,

    val version: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
