package com.startrace.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.startrace.core.database.dao.FragmentDao
import com.startrace.core.database.dao.LLMConfigDao
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.dao.StoryFragmentRefDao
import com.startrace.core.database.dao.UserDao
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.core.database.entity.LLMConfigEntity
import com.startrace.core.database.entity.StoryEntity
import com.startrace.core.database.entity.StoryFragmentRef
import com.startrace.core.database.entity.UserEntity

/**
 * V1 → V2 迁移：新增 story_fragment_refs 多对多关联表，
 * 并从 stories.fragment_ids_json 迁移已有数据。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS story_fragment_refs (
                story_id TEXT NOT NULL,
                fragment_id TEXT NOT NULL,
                PRIMARY KEY (story_id, fragment_id),
                FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
                FOREIGN KEY (fragment_id) REFERENCES fragments(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_story_fragment_refs_story_id ON story_fragment_refs(story_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_story_fragment_refs_fragment_id ON story_fragment_refs(fragment_id)")

        // 迁移已有数据：解析 fragment_ids_json → story_fragment_refs
        val cursor = db.query("SELECT id, fragment_ids_json FROM stories")
        while (cursor.moveToNext()) {
            val storyId = cursor.getString(0)
            val jsonStr = cursor.getString(1)
            if (jsonStr.isNullOrBlank() || jsonStr == "[]") continue
            try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val fid = arr.getString(i)
                    db.execSQL(
                        "INSERT OR REPLACE INTO story_fragment_refs (story_id, fragment_id) VALUES (?, ?)",
                        arrayOf(storyId, fid)
                    )
                }
            } catch (_: Exception) { /* skip malformed JSON */ }
        }
        cursor.close()
    }
}

/**
 * V2 → V3 迁移：新增 users 表，fragments 和 stories 添加 user_id 列
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE fragments ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE stories ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id TEXT NOT NULL PRIMARY KEY,
                username TEXT NOT NULL,
                token TEXT NOT NULL,
                joined_at INTEGER NOT NULL,
                last_login_at INTEGER NOT NULL
            )
        """)
    }
}

@Database(
    entities = [
        FragmentEntity::class,
        StoryEntity::class,
        LLMConfigEntity::class,
        StoryFragmentRef::class,
        UserEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fragmentDao(): FragmentDao
    abstract fun storyDao(): StoryDao
    abstract fun llmConfigDao(): LLMConfigDao
    abstract fun storyFragmentRefDao(): StoryFragmentRefDao
    abstract fun userDao(): UserDao
}
