package com.startrace.core.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── 请求体 ──────────────────────────────────

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

// ── 响应体 ──────────────────────────────────

/** 后端统一响应包装 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: T?
)

@JsonClass(generateAdapter = true)
data class AuthData(
    @Json(name = "token") val token: String,
    @Json(name = "userId") val userId: Long,
    @Json(name = "username") val username: String
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "id") val id: Long,
    @Json(name = "username") val username: String,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "fragmentCount") val fragmentCount: Int = 0,
    @Json(name = "storyCount") val storyCount: Int = 0
)
