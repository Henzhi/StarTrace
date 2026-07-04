package com.startrace.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 应用级 DataStore（单实例） */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Token 管理器 — DataStore 持久化 Sa-Token JWT
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
    }

    /** 观察 Token 变化 */
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }

    /** 观察用户 ID */
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }

    /** 观察用户名 */
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USERNAME] }

    /** 同步获取 Token */
    suspend fun getToken(): String? = context.dataStore.data.first()[Keys.TOKEN]

    /** 同步获取用户 ID */
    suspend fun getUserId(): String? = context.dataStore.data.first()[Keys.USER_ID]

    /** 保存登录信息 */
    suspend fun save(token: String, userId: String, username: String) {
        context.dataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.USER_ID] = userId
            it[Keys.USERNAME] = username
        }
    }

    /** 清除登录信息 */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    /** 更新 Token（刷新后） */
    suspend fun updateToken(token: String) {
        context.dataStore.edit { it[Keys.TOKEN] = token }
    }
}
