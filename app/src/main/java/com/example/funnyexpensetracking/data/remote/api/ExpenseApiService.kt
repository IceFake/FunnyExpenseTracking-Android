package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 记账数据同步API
 */
interface ExpenseApiService {

    @POST("transactions/sync")
    suspend fun syncTransactions(
        @Body request: SyncRequest
    ): Response<ApiResponse<List<TransactionDto>>>

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
        @Path("id") id: Long,
        @Body transaction: TransactionDto
    ): Response<ApiResponse<TransactionDto>>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>
}

