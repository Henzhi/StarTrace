package com.startrace.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.startrace.core.database.dao.FragmentDao
import com.startrace.core.database.entity.FragmentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * FragmentDao 集成测试 — Room in-memory database
 *
 * 覆盖场景：
 *  - CRUD：插入 → 查询 → 更新/归档 → 删除
 *  - Flow 响应式查询：observeAll / search / filterByDomain / observeCount
 *  - 边界情况：空表、重复 upsert、批量删除
 */
@RunWith(RobolectricTestRunner::class)
class FragmentDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FragmentDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = database.fragmentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== CRUD ====================

    @Test
    fun `upsert and getById — returns saved entity`() = runTest {
        val fragment = testFragment(id = "1", content = "赛博朋克城市的雨夜")

        dao.upsert(fragment)
        val result = dao.getById("1")

        assertNotNull(result)
        assertEquals("1", result!!.id)
        assertEquals("赛博朋克城市的雨夜", result.content)
        assertEquals("world", result.domainTag)
    }

    @Test
    fun `upsert with same id — replaces existing entity`() = runTest {
        val original = testFragment(id = "1", content = "原来的内容")
        val updated = testFragment(id = "1", content = "更新后的内容")

        dao.upsert(original)
        dao.upsert(updated)

        val result = dao.getById("1")
        assertEquals("更新后的内容", result!!.content)
        assertEquals(1, dao.observeCount().first())
    }

    @Test
    fun `getById for non-existent id — returns null`() = runTest {
        val result = dao.getById("does-not-exist")
        assertNull(result)
    }

    @Test
    fun `delete — removes entity`() = runTest {
        val fragment = testFragment(id = "1")
        dao.upsert(fragment)

        dao.delete(fragment)
        val result = dao.getById("1")

        assertNull(result)
        assertEquals(0, dao.observeCount().first())
    }

    @Test
    fun `deleteByIds — removes multiple entities`() = runTest {
        dao.upsert(testFragment(id = "1"))
        dao.upsert(testFragment(id = "2"))
        dao.upsert(testFragment(id = "3"))

        dao.deleteByIds(listOf("1", "3"))

        assertNull(dao.getById("1"))
        assertNotNull(dao.getById("2"))
        assertNull(dao.getById("3"))
    }

    // ==================== Archive ====================

    @Test
    fun `archive — excludes from observeAll`() = runTest {
        dao.upsert(testFragment(id = "1", isArchived = false))
        dao.upsert(testFragment(id = "2", isArchived = false))

        dao.archive("1")

        val active = dao.observeAll().first()
        assertEquals(1, active.size)
        assertEquals("2", active[0].id)
        assertEquals(1, dao.observeCount().first())
    }

    @Test
    fun `archive on already-archived — idempotent`() = runTest {
        dao.upsert(testFragment(id = "1", isArchived = true))
        dao.archive("1")  // should not throw
    }

    // ==================== Query ====================

    @Test
    fun `observeAll — returns unarchived entities sorted by createdAt desc`() = runTest {
        dao.upsert(testFragment(id = "1", content = "oldest", createdAt = 1000))
        dao.upsert(testFragment(id = "2", content = "newest", createdAt = 3000))
        dao.upsert(testFragment(id = "3", content = "middle", createdAt = 2000))
        dao.upsert(testFragment(id = "4", content = "archived", createdAt = 4000, isArchived = true))

        val result = dao.observeAll().first()

        assertEquals(3, result.size)
        assertEquals("newest", result[0].content)  // 3000 first
        assertEquals("middle", result[1].content)   // 2000 second
        assertEquals("oldest", result[2].content)   // 1000 last
    }

    @Test
    fun `search — finds by content keyword`() = runTest {
        dao.upsert(testFragment(id = "1", content = "赛博朋克的霓虹灯"))
        dao.upsert(testFragment(id = "2", content = "田园生活的宁静"))
        dao.upsert(testFragment(id = "3", content = "赛博空间与意识上传"))

        val results = dao.search("赛博").first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.content.contains("赛博") })
    }

    @Test
    fun `search — excludes archived fragments`() = runTest {
        dao.upsert(testFragment(id = "1", content = "赛博朋克", isArchived = true))

        val results = dao.search("赛博").first()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `search — no match returns empty`() = runTest {
        dao.upsert(testFragment(id = "1", content = "太空探索"))

        val results = dao.search("不存在的关键词").first()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `filterByDomain — returns matching domain`() = runTest {
        dao.upsert(testFragment(id = "1", domainTag = "world"))
        dao.upsert(testFragment(id = "2", domainTag = "character"))
        dao.upsert(testFragment(id = "3", domainTag = "world"))

        val results = dao.filterByDomain("world").first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.domainTag == "world" })
    }

    // ==================== Count ====================

    @Test
    fun `observeCount — tracks active fragments`() = runTest {
        assertEquals(0, dao.observeCount().first())

        dao.upsert(testFragment(id = "1"))
        dao.upsert(testFragment(id = "2"))

        assertEquals(2, dao.observeCount().first())

        dao.archive("1")
        assertEquals(1, dao.observeCount().first())
    }

    // ==================== Empty State ====================

    @Test
    fun `empty database — observeAll returns empty list`() = runTest {
        val result = dao.observeAll().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty database — observeCount returns zero`() = runTest {
        val count = dao.observeCount().first()
        assertEquals(0, count)
    }

    // ==================== Helpers ====================

    private fun testFragment(
        id: String,
        content: String = "测试碎片内容",
        domainTag: String = "world",
        formTag: String? = null,
        mood: String? = null,
        isArchived: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) = FragmentEntity(
        id = id,
        content = content,
        tagsJson = """["$domainTag", "${formTag ?: ""}"]""",
        domainTag = domainTag,
        formTag = formTag,
        mood = mood,
        isArchived = isArchived,
        createdAt = createdAt,
        positionX = 100f,
        positionY = 200f
    )
}
