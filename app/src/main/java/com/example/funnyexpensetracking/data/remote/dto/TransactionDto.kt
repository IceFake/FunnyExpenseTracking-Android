package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 通用API响应包装
 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
)

/**
 * 交易记录DTO
 */
data class TransactionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("category") val category: String,
    @SerializedName("note") val note: String = "",
    @SerializedName("date") val date: Long,
    @SerializedName("created_at") val createdAt: Long
)

/**
 * 固定收支DTO
 */
data class FixedIncomeDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("frequency") val frequency: String
)

/**
 * 同步请求
 */
data class SyncRequest(
    @SerializedName("transactions") val transactions: List<TransactionDto>,
    @SerializedName("last_sync_time") val lastSyncTime: Long
)

