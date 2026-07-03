package com.startrace.core.data.repository

import com.startrace.core.database.dao.FragmentDao
import com.startrace.core.database.entity.FragmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 碎片数据仓库 — 离线优先，所有操作直接走 Room
 */
@Singleton
class FragmentRepository @Inject constructor(
    private val fragmentDao: FragmentDao
) {
    /** 观察所有未归档碎片 */
    fun observeAll(): Flow<List<FragmentEntity>> = fragmentDao.observeAll()

    /** 搜索碎片 */
    fun search(query: String): Flow<List<FragmentEntity>> = fragmentDao.search(query)

    /** 按领域标签筛选 */
    fun filterByDomain(domain: String): Flow<List<FragmentEntity>> = fragmentDao.filterByDomain(domain)

    /** 碎片数量 */
    fun observeCount(): Flow<Int> = fragmentDao.observeCount()

    /** 根据 ID 获取单个碎片 */
    suspend fun getById(id: String): FragmentEntity? = fragmentDao.getById(id)

    /**
     * 创建碎片 — 自动分配星系坐标（黄金角螺旋算法）
     *
     * 同标签的碎片会在螺旋上聚集：domainTag 的 hashCode 决定起始偏移，
     * 同 domain 内的碎片按创建顺序在附近螺旋展开。
     */
    suspend fun create(
        content: String,
        domainTag: String,
        formTag: String? = null,
        mood: String? = null,
        source: String = "text"
    ): FragmentEntity {
        val count = fragmentDao.countByDomain(domainTag)
        val (x, y) = spiralCoordinate(domainTag, count)

        return FragmentEntity(
            id = UUID.randomUUID().toString(),
            content = content,
            tagsJson = "[]",
            domainTag = domainTag,
            formTag = formTag,
            mood = mood,
            source = source,
            positionX = x,
            positionY = y,
            createdAt = System.currentTimeMillis()
        ).also { fragmentDao.upsert(it) }
    }

    /** 更新碎片内容 */
    suspend fun update(fragment: FragmentEntity) = fragmentDao.upsert(fragment)

    /** 归档碎片 */
    suspend fun archive(id: String) = fragmentDao.archive(id)

    /** 删除碎片 */
    suspend fun delete(fragment: FragmentEntity) = fragmentDao.delete(fragment)

    /** 批量删除 */
    suspend fun deleteByIds(ids: List<String>) = fragmentDao.deleteByIds(ids)

    /** 批量归档 */
    suspend fun archiveByIds(ids: List<String>) = fragmentDao.archiveByIds(ids)

    /**
     * 黄金角螺旋坐标算法
     *
     * 使用 φ ≈ 137.508°（黄金角）生成自然螺旋分布：
     * - angle = i × φ + hashOffset（同 domain 碎片聚集）
     * - radius = √(i+1) × spacing（均匀扩散）
     *
     * 画布坐标范围：±1000 单位
     */
    private fun spiralCoordinate(domainTag: String, indexInDomain: Int): Pair<Float, Float> {
        val goldenAngle = Math.toRadians(137.508)
        val domainHash = domainTag.hashCode().toDouble()
        val offset = (domainHash % 360) * Math.PI / 180.0  // 各领域在螺旋上均匀错开

        val i = (indexInDomain + 1).toDouble()
        val angle = i * goldenAngle + offset
        val radius = sqrt(i) * 80f  // 间距 80 单位

        val x = (radius * cos(angle)).toFloat()
        val y = (radius * sin(angle)).toFloat()

        return Pair(x, y)
    }
}
