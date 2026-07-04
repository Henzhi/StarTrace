package com.startrace.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 灵感碎片实体
 */
@Entity(tableName = "fragments")
data class FragmentEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String = "",         // 关联用户，"" 表示未登录

    val content: String,

    @ColumnInfo(name = "tags_json")
    val tagsJson: String,           // JSON Array: ["科幻","人物设定"]

    @ColumnInfo(name = "domain_tag")
    val domainTag: String,          // 灵感领域（必选）

    @ColumnInfo(name = "form_tag")
    val formTag: String? = null,    // 内容形态（可选）

    val mood: String? = null,       // excited/calm/confused/amazed

    val source: String = "text",

    val positionX: Float = 0f,
    val positionY: Float = 0f,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
