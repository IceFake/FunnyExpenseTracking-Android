package com.example.funnyexpensetracking.domain.repository

/**
 * 与 funny-expense-backend 的 JWT 认证对接。
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String, nickname: String): Result<Unit>
    suspend fun logout()
}
