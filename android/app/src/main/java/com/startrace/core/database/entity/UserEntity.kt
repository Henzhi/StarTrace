package com.startrace.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户实体 — 本地缓存登录用户信息
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,

    val username: String,

    val token: String,

    @ColumnInfo(name = "avatar_path")
    val avatarPath: String = "",

    @ColumnInfo(name = "joined_at")
    val joinedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_login_at")
    val lastLoginAt: Long = System.currentTimeMillis()
)
