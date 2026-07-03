package com.startrace.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key 安全存储管理器。
 *
 * 使用 AndroidKeyStore + AES-256/GCM 加密 API Key：
 * - 密钥由硬件 KeyStore 保护，不离开设备
 * - IV 随机生成，每次加密不同
 * - 密文以 Base64 编码存入 SharedPreferences
 *
 * 反编译后无法获取明文 Key（密钥在 TEE/StrongBox 中）。
 */
@Singleton
class KeyStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "startrace_llm_key"
        private const val PREFS_NAME = "startrace_keystore_prefs"
        private const val KEY_PREFIX = "apikey_"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12 // GCM standard
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 获取或创建 AES 密钥 */
    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return if (ks.containsAlias(KEYSTORE_ALIAS)) {
            (ks.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
            }.generateKey()
        }
    }

    /**
     * 加密并持久化 API Key。
     * @param configId LLM 配置 ID（用作存储 key）
     * @param apiKey 明文 API Key
     */
    fun encryptAndSave(configId: String, apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

        // 存储：IV + 密文（用冒号分隔的 Base64）
        val encoded = Base64.encodeToString(iv, Base64.NO_WRAP) +
                ":" + Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        prefs.edit().putString(KEY_PREFIX + configId, encoded).apply()
    }

    /**
     * 解密获取 API Key。
     * @return 明文 API Key，不存在则返回 null
     */
    fun decrypt(configId: String): String? {
        val encoded = prefs.getString(KEY_PREFIX + configId, null) ?: return null
        val parts = encoded.split(":", limit = 2)
        if (parts.size != 2) return null

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /** 删除存储的 API Key */
    fun delete(configId: String) {
        prefs.edit().remove(KEY_PREFIX + configId).apply()
    }
}
