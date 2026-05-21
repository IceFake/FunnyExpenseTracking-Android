package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.config.ApiEnvironmentConfig
import com.example.funnyexpensetracking.data.remote.api.AuthApiService
import com.example.funnyexpensetracking.data.remote.dto.LoginRequestDto
import com.example.funnyexpensetracking.data.remote.dto.LogoutRequestDto
import com.example.funnyexpensetracking.data.remote.dto.RegisterRequestDto
import com.example.funnyexpensetracking.domain.repository.AuthRepository
import java.io.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val userPreferencesManager: UserPreferencesManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.login(
                LoginRequestDto(
                    email = email.trim(),
                    password = password,
                    deviceId = userPreferencesManager.getOrCreateDeviceId()
                )
            )
            val body = response.body()
            if (response.isSuccessful && body?.code == 200 && !body.data?.token.isNullOrBlank()) {
                val data = requireNotNull(body.data)
                userPreferencesManager.saveAuthToken(data.token)
                if (!data.refreshToken.isNullOrBlank()) {
                    userPreferencesManager.saveRefreshToken(data.refreshToken)
                }
                data.user?.id?.let { userPreferencesManager.saveBackendUserId(it.toString()) }
                data.user?.email?.let { userPreferencesManager.saveBackendUserEmail(it) }
                Result.success(Unit)
            } else {
                var errorMessage = body?.message
                if (errorMessage.isNullOrBlank()) {
                    val errorString = response.errorBody()?.string()
                    if (!errorString.isNullOrBlank()) {
                        try {
                            val errorJson = org.json.JSONObject(errorString)
                            errorMessage = errorJson.optString("message").takeIf { it.isNotBlank() }
                        } catch (ignore: Exception) {}
                    }
                }
                
                // 拦截常见的 Spring Security 报错信息或相关认证状态码
                if (response.code() == 401 || response.code() == 403 || 
                    errorMessage?.contains("Bad credential", ignoreCase = true) == true) {
                    return Result.failure(Exception("邮箱不存在或密码错误"))
                }
                
                Result.failure(Exception(errorMessage ?: response.message() ?: "登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("登录失败"), e))
        }
    }

    override suspend fun register(email: String, password: String, nickname: String): Result<Unit> {
        return try {
            val response = authApiService.register(
                RegisterRequestDto(email.trim(), password, nickname.trim())
            )
            val body = response.body()
            if (response.isSuccessful && body?.code == 200) {
                body.data?.id?.let { userPreferencesManager.saveBackendUserId(it.toString()) }
                body.data?.email?.let { userPreferencesManager.saveBackendUserEmail(it) }
                Result.success(Unit)
            } else {
                if (response.code() == 401) {
                    return Result.failure(Exception("邮箱不存在或密码错误"))
                }
                var errorMessage = body?.message
                if (errorMessage.isNullOrBlank()) {
                    val errorString = response.errorBody()?.string()
                    if (!errorString.isNullOrBlank()) {
                        try {
                            val errorJson = org.json.JSONObject(errorString)
                            errorMessage = errorJson.optString("message").takeIf { it.isNotBlank() }
                        } catch (e: Exception) {
                            // Ignore parsing error
                        }
                    }
                }
                Result.failure(Exception(errorMessage ?: response.message() ?: "注册失败"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("注册失败"), e))
        }
    }

    override suspend fun logout() {
        try {
            val refreshToken = userPreferencesManager.getRefreshToken()
            if (refreshToken.isNotBlank()) {
                authApiService.logout(LogoutRequestDto(refreshToken = refreshToken))
            }
        } catch (_: Exception) {
            // 退出登录时优先清理本地会话，远端失败不阻塞
        } finally {
            userPreferencesManager.clearBackendSession()
        }
    }

    private fun Throwable.toUserMessage(defaultMessage: String): String {
        return when (this) {
            is IOException -> {
                if (ApiEnvironmentConfig.IS_TEST_ENV) {
                    "本地测试后端无法连接，请确认服务已启动且可访问 10.0.2.2:8081"
                } else {
                    "网络连接失败，请检查网络后重试"
                }
            }
            is HttpException -> {
                if (code() == 401 || code() == 403) "邮箱不存在或密码错误"
                else "服务器错误(${code()})，请稍后重试"
            }
            else -> {
                val msg = message
                if (msg?.contains("Bad credential", ignoreCase = true) == true) {
                    "邮箱不存在或密码错误"
                } else {
                    msg?.takeIf { it.isNotBlank() } ?: defaultMessage
                }
            }
        }
    }
}
