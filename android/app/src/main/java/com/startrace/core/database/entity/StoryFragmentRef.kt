package com.startrace.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 故事-碎片 多对多关联表。
 *
 * 一个灵感碎片可以参与多个故事的生成，
 * 一个故事可以由多个碎片组合而成。
 */
@Entity(
    tableName = "story_fragment_refs",
    primaryKeys = ["story_id", "fragment_id"],
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FragmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["fragment_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("story_id"),
        Index("fragment_id")
    ]
)
data class StoryFragmentRef(
    @ColumnInfo(name = "story_id")
    val storyId: String,

    @ColumnInfo(name = "fragment_id")
    val fragmentId: String
)
