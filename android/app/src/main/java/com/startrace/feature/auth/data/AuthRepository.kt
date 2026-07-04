package com.startrace.feature.auth.data

import com.startrace.core.database.dao.UserDao
import com.startrace.core.database.entity.UserEntity
import com.startrace.core.network.TokenManager
import com.startrace.core.network.api.AuthApi
import com.startrace.core.network.api.LoginRequest
import com.startrace.core.network.api.RegisterRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证数据仓库 — 协调网络 API 与本地缓存
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val userDao: UserDao
) {
    /** 注册 */
    suspend fun register(username: String, password: String): Result<String> {
        return try {
            val response = authApi.register(RegisterRequest(username, password))
            if (response.code != 200 || response.data == null) {
                return Result.failure(Exception(response.message ?: "注册失败"))
            }
            val data = response.data
            // 持久化
            tokenManager.save(data.token, data.userId.toString(), data.username)
            userDao.upsert(
                UserEntity(
                    id = data.userId.toString(),
                    username = data.username,
                    token = data.token
                )
            )
            Result.success(data.username)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 登录 */
    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            if (response.code != 200 || response.data == null) {
                return Result.failure(Exception(response.message ?: "登录失败"))
            }
            val data = response.data
            tokenManager.save(data.token, data.userId.toString(), data.username)
            userDao.upsert(
                UserEntity(
                    id = data.userId.toString(),
                    username = data.username,
                    token = data.token
                )
            )
            Result.success(data.username)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 退出登录 */
    suspend fun logout() {
        tokenManager.clear()
        userDao.logout()
    }

    /** 当前是否已登录 */
    suspend fun isLoggedIn(): Boolean = tokenManager.getToken() != null

    /** 获取当前用户 ID */
    suspend fun getUserId(): String? = tokenManager.getUserId()

    /** 获取当前用户名 */
    suspend fun getUsername(): String? = tokenManager.tokenFlow.let {
        null  // 使用 TokenManager.usernameFlow
    }

    /** 从本地缓存获取已登录用户 */
    suspend fun getCachedUser(): UserEntity? = userDao.getLoggedIn()
}
