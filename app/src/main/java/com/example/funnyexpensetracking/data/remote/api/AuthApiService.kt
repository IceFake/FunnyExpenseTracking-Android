package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.ApiResponse
import com.example.funnyexpensetracking.data.remote.dto.LoginRequestDto
import com.example.funnyexpensetracking.data.remote.dto.LoginResponseDto
import com.example.funnyexpensetracking.data.remote.dto.RegisterRequestDto
import com.example.funnyexpensetracking.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<ApiResponse<LoginResponseDto>>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<ApiResponse<UserDto>>
}
