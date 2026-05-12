package com.example.funnyexpensetracking.data.remote

import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 为业务后端请求附加 JWT（与 Spring Security Bearer 约定一致）。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = userPreferencesManager.getAuthToken().trim()
        val request = if (token.isNotEmpty()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
