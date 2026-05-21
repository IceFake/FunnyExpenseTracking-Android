@file:Suppress("unused")

package com.example.funnyexpensetracking.data.remote.api

import com.example.funnyexpensetracking.data.remote.dto.AccountDto
import com.example.funnyexpensetracking.data.remote.dto.AccountSyncRequest
import com.example.funnyexpensetracking.data.remote.dto.ApiResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * 账户同步 API（与 funny-expense-backend 的 /accounts 对齐）
 *
 * 后端 Account ID 为 UUID 字符串，因此所有 id 参数均为 String 类型。
 */
@Suppress("unused")
interface AccountApiService {

    @POST("accounts/sync")
    suspend fun syncAccounts(@Body request: AccountSyncRequest): Response<ApiResponse<List<AccountDto>>>

    @GET("accounts")
    suspend fun getAccounts(): Response<ApiResponse<List<AccountDto>>>

    @POST("accounts")
    suspend fun createAccount(@Body account: AccountDto): Response<ApiResponse<AccountDto>>

    @PUT("accounts/{id}")
    suspend fun updateAccount(@Path("id") id: String, @Body account: AccountDto): Response<ApiResponse<AccountDto>>

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<ApiResponse<Unit>>
}
