package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 股票行情API
 */
interface StockApiService {

    @GET("stock/quote/{symbol}")
    suspend fun getQuote(
        @Path("symbol") symbol: String
    ): Response<ApiResponse<StockQuoteDto>>

    @GET("stock/realtime/{symbol}")
    suspend fun getRealtimeQuote(
        @Path("symbol") symbol: String
    ): Response<ApiResponse<StockQuoteDto>>

    @POST("stock/quotes")
    suspend fun getBatchQuotes(
        @Body request: BatchQuoteRequest
    ): Response<ApiResponse<BatchQuoteResponse>>

    @GET("stock/search")
    suspend fun searchStock(
        @Query("keyword") keyword: String
    ): Response<ApiResponse<List<StockSearchResult>>>
}

