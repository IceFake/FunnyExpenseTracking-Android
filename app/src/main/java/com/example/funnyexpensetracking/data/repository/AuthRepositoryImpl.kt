package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.remote.api.AuthApiService
import com.example.funnyexpensetracking.data.remote.dto.LoginRequestDto
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
            val response = authApiService.login(LoginRequestDto(email.trim(), password))
            val body = response.body()
            if (response.isSuccessful && body?.code == 200 && !body.data?.token.isNullOrBlank()) {
                val data = requireNotNull(body.data)
                userPreferencesManager.saveAuthToken(data.token)
                if (!data.refreshToken.isNullOrBlank()) {
                    userPreferencesManager.saveRefreshToken(data.refreshToken)
                }
                userPreferencesManager.saveBackendUserEmail(email.trim())
                Result.success(Unit)
            } else {
                Result.failure(Exception(body?.message ?: response.message() ?: "登录失败"))
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
                Result.success(Unit)
            } else {
                Result.failure(Exception(body?.message ?: response.message() ?: "注册失败"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage("注册失败"), e))
        }
    }

    override fun logout() {
        userPreferencesManager.clearBackendSession()
    }

    private fun Throwable.toUserMessage(defaultMessage: String): String {
        return when (this) {
            is IOException -> "网络连接失败，请检查网络后重试"
            is HttpException -> "服务器错误(${code()})，请稍后重试"
            else -> message?.takeIf { it.isNotBlank() } ?: defaultMessage
        }
    }
}
