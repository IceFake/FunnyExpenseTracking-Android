package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.ApiResponse
import com.example.funnyexpensetracking.data.remote.dto.LoginResponseDto
import com.example.funnyexpensetracking.data.remote.dto.RefreshTokenRequestDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 仅用于刷新 Token 的接口（使用无 Authenticator 的 OkHttp，避免 401 循环）。
 */
interface AuthTokenRefreshApiService {

    @POST("auth/refresh")
    fun refresh(@Body body: RefreshTokenRequestDto): Call<ApiResponse<LoginResponseDto>>
}
