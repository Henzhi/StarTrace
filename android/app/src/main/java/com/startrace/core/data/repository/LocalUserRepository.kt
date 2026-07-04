package com.startrace.core.data.repository

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.startrace.core.database.dao.UserDao
import com.startrace.core.database.entity.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalUserRepository @Inject constructor(
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) {

    private val DEFAULT_USERNAME = "星辰旅人"
    private val PREF_KEY_DEVICE_ID = "device_user_id"
    private val PREF_KEY_UUID = "fallback_uuid"

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "startrace_user_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val userFlow: Flow<UserEntity?> = userDao.observeLoggedIn()

    val usernameFlow: Flow<String> = userFlow.map { it?.username ?: DEFAULT_USERNAME }

    val isLoggedInFlow: Flow<Boolean> = userFlow.map { it != null }

    val userIdFlow: Flow<String> = userFlow.map { it?.id ?: getDeviceUserId() }

    private fun getDeviceUserId(): String {
        var userId = sharedPreferences.getString(PREF_KEY_DEVICE_ID, null)
        if (userId != null) {
            return userId
        }

        userId = generateDeviceBasedId()
        sharedPreferences.edit().putString(PREF_KEY_DEVICE_ID, userId).apply()
        return userId
    }

    private fun generateDeviceBasedId(): String {
        try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrEmpty()) {
                return hashString(androidId)
            }
        } catch (e: Exception) {
        }

        var fallbackUuid = sharedPreferences.getString(PREF_KEY_UUID, null)
        if (fallbackUuid == null) {
            fallbackUuid = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(PREF_KEY_UUID, fallbackUuid).apply()
        }
        return fallbackUuid
    }

    private fun hashString(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray())
            hash.joinToString("") { "%02x".format(it) }.substring(0, 32)
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    suspend fun ensureLocalUser(): UserEntity {
        var user = userDao.getLoggedIn()
        if (user == null) {
            user = UserEntity(
                id = getDeviceUserId(),
                username = DEFAULT_USERNAME,
                token = "",
                joinedAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
            userDao.upsert(user)
        }
        return user
    }

    suspend fun getUserId(): String {
        return ensureLocalUser().id
    }

    suspend fun getUsername(): String {
        return ensureLocalUser().username
    }

    suspend fun updateUsername(username: String) {
        val user = ensureLocalUser()
        userDao.upsert(user.copy(username = username.trim(), lastLoginAt = System.currentTimeMillis()))
    }

    suspend fun getCachedUser(): UserEntity? = userDao.getLoggedIn()
}