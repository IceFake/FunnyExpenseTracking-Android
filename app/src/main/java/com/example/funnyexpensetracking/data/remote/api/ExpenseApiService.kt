@file:Suppress("unused")

package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 记账数据同步 API（与 funny-expense-backend 一致，context-path 已含在 baseUrl 的 /v1/）
 *
 * 后端 Transaction ID 为 UUID 字符串，因此所有 id 参数均为 String 类型。
 */
@Suppress("unused")
interface ExpenseApiService {

    @POST("transactions/sync")
    suspend fun syncTransactions(
        @Body request: SyncRequest
    ): Response<ApiResponse<SyncResponseDto>>

    @GET("transactions")
    suspend fun getTransactions(
        @Query("start_date") startDate: Long,
        @Query("end_date") endDate: Long
    ): Response<ApiResponse<List<TransactionDto>>>

    @POST("transactions")
    suspend fun createTransaction(
        @Body transaction: TransactionDto
    ): Response<ApiResponse<TransactionDto>>

    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body transaction: TransactionDto
    ): Response<ApiResponse<TransactionDto>>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}
