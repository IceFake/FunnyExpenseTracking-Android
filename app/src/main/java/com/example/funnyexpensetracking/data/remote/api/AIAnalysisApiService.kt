package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * AI分析API
 */
interface AIAnalysisApiService {

    @POST("ai/analyze")
    suspend fun analyzeHabits(
        @Body request: AIAnalysisRequest
    ): Response<ApiResponse<AIAnalysisResultDto>>

    @GET("ai/suggestions")
    suspend fun getSuggestions(): Response<ApiResponse<List<SuggestionDto>>>

    @GET("ai/history")
    suspend fun getAnalysisHistory(
        @Query("limit") limit: Int
    ): Response<ApiResponse<List<AIAnalysisResultDto>>>

    @GET("ai/result/{id}")
    suspend fun getAnalysisResult(
        @Path("id") analysisId: String
    ): Response<ApiResponse<AIAnalysisResultDto>>
}

