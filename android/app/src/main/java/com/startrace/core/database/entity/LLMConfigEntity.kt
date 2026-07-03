package com.startrace.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自配 LLM 配置实体
 *
 * 注意：apiKey 不在此 Entity 中，由 Android Keystore 独立加密存储。
 */
@Entity(tableName = "llm_configs")
data class LLMConfigEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val apiUrl: String,
    val modelName: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
