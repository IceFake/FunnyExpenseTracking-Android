package com.example.funnyexpensetracking.data.remote

import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.remote.api.AuthTokenRefreshApiService
import com.example.funnyexpensetracking.data.remote.dto.RefreshTokenRequestDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步刷新 Access Token（供 [TokenAuthenticator] 使用，避免与带 Authenticator 的 Retrofit 互相依赖）。
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val refreshApi: AuthTokenRefreshApiService,
    private val userPreferencesManager: UserPreferencesManager
) {

    private val lock = Any()

    /**
     * @return 是否已成功写入新的 access（及可选的新 refresh）
     */
    fun refreshBlocking(): Boolean = synchronized(lock) {
        val refresh = userPreferencesManager.getRefreshToken().trim()
        if (refresh.isEmpty()) return false

        val resp = try {
            refreshApi.refresh(RefreshTokenRequestDto(refresh)).execute()
        } catch (_: Exception) {
            return false
        }

        val body = resp.body()
        val ok = resp.isSuccessful && body?.code == 200 && body.data != null
        if (ok) {
            val data = body.data!!
            if (!data.token.isNullOrBlank()) {
                userPreferencesManager.saveAuthToken(data.token)
            }
            if (!data.refreshToken.isNullOrBlank()) {
                userPreferencesManager.saveRefreshToken(data.refreshToken)
            }
            return userPreferencesManager.getAuthToken().isNotBlank()
        }

        if (resp.code() == 401 || body?.code == 401) {
            userPreferencesManager.clearBackendSession()
        }
        return false
    }

    /** Access 仍有效则 true；若只有 Refresh 则尝试 [refreshBlocking]。 */
    fun ensureAccessOrRefresh(): Boolean {
        if (userPreferencesManager.getAuthToken().isNotBlank()) return true
        return refreshBlocking()
    }
}
