package com.startrace.core.network

import com.startrace.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp 拦截器 — 自动附加 JWT Token 到请求头
 *
 * 对认证白名单路径（register/login）不附加 Token
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        private val SKIP_AUTH_PATHS = setOf(
            "/api/v1/auth/register",
            "/api/v1/auth/login"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 白名单路径跳过 Token 附加
        val path = request.url.encodedPath
        if (SKIP_AUTH_PATHS.any { path.startsWith(it) }) {
            return chain.proceed(request)
        }

        // 同步读取 Token（okhttp 拦截器运行在非协程线程）
        val token = runBlocking { tokenManager.tokenFlow.first() }
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }

        val newRequest = request.newBuilder()
            .header("Authorization", token)
            .build()
        return chain.proceed(newRequest)
    }
}
