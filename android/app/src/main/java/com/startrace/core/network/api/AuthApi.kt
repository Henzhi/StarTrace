package com.startrace.core.network.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 认证 API — 对接后端 AuthController
 */
interface AuthApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthData>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthData>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(): ApiResponse<AuthData>

    @GET("api/v1/users/me")
    suspend fun getProfile(): ApiResponse<UserProfile>
}
