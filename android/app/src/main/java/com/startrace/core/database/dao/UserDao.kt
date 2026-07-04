package com.startrace.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.startrace.core.database.entity.UserEntity

/**
 * 用户 DAO — 本地缓存登录状态
 */
@Dao
interface UserDao {

    /** 插入或替换当前用户（单用户模式，同时只存一个） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    /** 获取已登录用户（若有） */
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getLoggedIn(): UserEntity?

    /** 观察已登录用户 */
    @Query("SELECT * FROM users LIMIT 1")
    fun observeLoggedIn(): kotlinx.coroutines.flow.Flow<UserEntity?>

    /** 退出登录（清除本地用户数据） */
    @Query("DELETE FROM users")
    suspend fun logout()

    /** 更新 Token */
    @Query("UPDATE users SET token = :token, last_login_at = :ts WHERE id = :userId")
    suspend fun updateToken(userId: String, token: String, ts: Long = System.currentTimeMillis())
}
