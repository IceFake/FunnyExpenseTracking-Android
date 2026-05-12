package com.example.funnyexpensetracking.data.remote

import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收到 401 时用 Refresh Token 换新的 Access Token 并重试请求（OkHttp [Authenticator]）。
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher,
    private val userPreferencesManager: UserPreferencesManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.contains("/auth/refresh") || path.contains("/auth/login") || path.contains("/auth/register")) {
            return null
        }
        if (response.request.header("Authorization") == null) {
            return null
        }
        if (retryCount(response) >= 2) {
            return null
        }

        if (!tokenRefresher.refreshBlocking()) {
            return null
        }

        val newAccess = userPreferencesManager.getAuthToken().trim()
        if (newAccess.isEmpty()) return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    private fun retryCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
