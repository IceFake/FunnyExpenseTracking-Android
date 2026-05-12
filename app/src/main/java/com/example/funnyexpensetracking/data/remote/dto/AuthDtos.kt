package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    /** Access JWT */
    val token: String,
    /** Refresh JWT（仅用于 /auth/refresh） */
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("tokenType") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null
)

data class RefreshTokenRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val nickname: String
)

data class UserDto(
    val id: String? = null,
    val email: String? = null,
    val nickname: String? = null,
    @SerializedName("createdAt") val createdAt: Long? = null
)
