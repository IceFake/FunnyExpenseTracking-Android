package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    val email: String,
    val password: String,
    @SerializedName(value = "device_id", alternate = ["deviceId"]) val deviceId: String
)

data class LoginResponseDto(
    /** Access JWT */
    @SerializedName(value = "access_token", alternate = ["token"]) val token: String,
    /** Refresh JWT（仅用于 /auth/refresh） */
    @SerializedName(value = "refresh_token", alternate = ["refreshToken"]) val refreshToken: String? = null,
    @SerializedName(value = "token_type", alternate = ["tokenType"]) val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null,
    @SerializedName("user") val user: UserDto? = null
)

data class RefreshTokenRequestDto(
    @SerializedName(value = "refresh_token", alternate = ["refreshToken"]) val refreshToken: String
)

data class LogoutRequestDto(
    @SerializedName(value = "refresh_token", alternate = ["refreshToken"]) val refreshToken: String? = null,
    @SerializedName(value = "session_id", alternate = ["sessionId"]) val sessionId: Long? = null
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val nickname: String,
    val phone: String? = null
)

data class UserDto(
    val id: Long? = null,
    val email: String? = null,
    val nickname: String? = null,
    @SerializedName(value = "created_at", alternate = ["createdAt"]) val createdAt: Long? = null,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"]) val updatedAt: Long? = null
)
