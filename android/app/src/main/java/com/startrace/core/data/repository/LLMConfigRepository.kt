package com.startrace.core.data.repository

import com.startrace.core.database.dao.LLMConfigDao
import com.startrace.core.database.entity.LLMConfigEntity
import com.startrace.core.security.KeyStoreManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLM 配置数据仓库。
 *
 * 封装 Room DAO + KeyStore API Key 读写，
 * 提供配置的增删改查、默认切换、连接测试等操作。
 */
@Singleton
class LLMConfigRepository @Inject constructor(
    private val dao: LLMConfigDao,
    private val keyStore: KeyStoreManager
) {
    /** 观察所有配置（实时 Flow） */
    fun observeAll(): Flow<List<LLMConfigEntity>> = dao.observeAll()

    /** 获取默认配置 */
    suspend fun getDefault(): LLMConfigEntity? = dao.getDefault()

    /**
     * 创建或更新配置。
     * @param id 为 null 时视为新建，否则为更新
     * @param apiKey 明文 API Key（保存时加密，读取时不填写则保留原有值）
     */
    suspend fun save(
        id: String? = null,
        name: String,
        apiUrl: String,
        modelName: String,
        apiKey: String?,
        isDefault: Boolean = false
    ) {
        val configId = id ?: UUID.randomUUID().toString()

        // 如果设为默认，先清除其他默认标记
        if (isDefault) dao.clearDefault()

        dao.upsert(
            LLMConfigEntity(
                id = configId,
                name = name,
                apiUrl = apiUrl,
                modelName = modelName,
                isDefault = isDefault,
                createdAt = System.currentTimeMillis()
            )
        )

        // 保存 API Key（仅在提供新 key 时加密存储）
        if (!apiKey.isNullOrBlank()) {
            keyStore.encryptAndSave(configId, apiKey)
        }
    }

    /** 获取某配置的解密 API Key */
    fun getApiKey(configId: String): String? = keyStore.decrypt(configId)

    /** 删除配置及其关联的 API Key */
    suspend fun delete(config: LLMConfigEntity) {
        dao.delete(config)
        keyStore.delete(config.id)
    }
}
