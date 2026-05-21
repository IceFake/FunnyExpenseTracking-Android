package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 股票行情API — 仅保留后端实际实现的端点
 * 批量行情: POST /stock/quotes
 */
interface StockApiService {

    @POST("stock/quotes")
    suspend fun getBatchQuotes(
        @Body request: BatchQuoteRequest
    ): Response<ApiResponse<BatchQuoteResponse>>
}
