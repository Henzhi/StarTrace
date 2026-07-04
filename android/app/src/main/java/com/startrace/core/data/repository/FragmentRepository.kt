package com.startrace.core.data.repository

import com.startrace.core.database.dao.FragmentDao
import com.startrace.core.database.entity.FragmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class FragmentRepository @Inject constructor(
    private val fragmentDao: FragmentDao,
    private val userRepository: LocalUserRepository
) {

    private val userIdFlow: Flow<String> = userRepository.userFlow.map { it?.id ?: "" }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeAll(): Flow<List<FragmentEntity>> =
        userIdFlow.flatMapLatest { uid -> fragmentDao.observeAll(uid) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun search(query: String): Flow<List<FragmentEntity>> =
        userIdFlow.flatMapLatest { uid -> fragmentDao.search(query, uid) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun filterByDomain(domain: String): Flow<List<FragmentEntity>> =
        userIdFlow.flatMapLatest { uid -> fragmentDao.filterByDomain(domain, uid) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeCount(): Flow<Int> =
        userIdFlow.flatMapLatest { uid -> fragmentDao.observeCount(uid) }

    suspend fun getById(id: String): FragmentEntity? = fragmentDao.getById(id)

    private suspend fun currentUserId(): String = userRepository.getUserId()

    suspend fun create(
        content: String,
        domainTag: String,
        formTag: String? = null,
        mood: String? = null,
        source: String = "text"
    ): FragmentEntity {
        val uid = currentUserId()
        val count = fragmentDao.countByDomain(domainTag, uid)
        val (x, y) = spiralCoordinate(domainTag, count)

        return FragmentEntity(
            id = UUID.randomUUID().toString(),
            userId = uid,
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

    suspend fun update(fragment: FragmentEntity) = fragmentDao.upsert(fragment)

    suspend fun archive(id: String) = fragmentDao.archive(id)

    suspend fun delete(fragment: FragmentEntity) = fragmentDao.delete(fragment)

    suspend fun deleteByIds(ids: List<String>) = fragmentDao.deleteByIds(ids)

    suspend fun archiveByIds(ids: List<String>) = fragmentDao.archiveByIds(ids)

    private fun spiralCoordinate(domainTag: String, indexInDomain: Int): Pair<Float, Float> {
        val goldenAngle = Math.toRadians(137.508)
        val domainHash = domainTag.hashCode().toDouble()
        val offset = (domainHash % 360) * Math.PI / 180.0

        val i = (indexInDomain + 1).toDouble()
        val angle = i * goldenAngle + offset
        val radius = sqrt(i) * 80f

        val x = (radius * cos(angle)).toFloat()
        val y = (radius * sin(angle)).toFloat()

        return Pair(x, y)
    }
}