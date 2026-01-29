package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 统计数据API
 */
interface StatisticsApiService {

    @POST("statistics/monthly")
    suspend fun getMonthlyStatistics(
        @Body request: StatisticsRequest
    ): Response<ApiResponse<StatisticsDto>>

    @POST("statistics/yearly")
    suspend fun getYearlyStatistics(
        @Body request: StatisticsRequest
    ): Response<ApiResponse<StatisticsDto>>

    @GET("statistics/category")
    suspend fun getCategoryStatistics(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<ApiResponse<List<CategoryStatDto>>>

    @GET("statistics/trend")
    suspend fun getTrendStatistics(
        @Query("months") months: Int
    ): Response<ApiResponse<List<StatisticsDto>>>
}

