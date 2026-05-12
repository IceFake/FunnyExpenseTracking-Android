package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 通用API响应包装（与后端 [ApiResponse] 对齐）
 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
)

/**
 * 交易记录 DTO（与后端 TransactionDto 的 JSON 命名一致：camelCase）
 */
data class TransactionDto(
    @SerializedName("id") var id: Long? = null,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("category") val category: String,
    @SerializedName(value = "account_id", alternate = ["accountId"]) val accountId: Long? = null,
    @SerializedName("note") val note: String = "",
    @SerializedName("date") val date: Long,
    @SerializedName(value = "created_at", alternate = ["createdAt"]) val createdAt: Long? = null,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"]) val updatedAt: Long? = null,
    @SerializedName(value = "version", alternate = ["version"]) val version: Long? = null,
    @SerializedName(value = "deleted_at", alternate = ["deletedAt"]) val deletedAt: Long? = null
)

/**
 * 账户 DTO（与后端 AccountDto 对齐）
 */
data class AccountDto(
    @SerializedName("id") var id: Long? = null,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String = "",
    @SerializedName("balance") val balance: Double = 0.0,
    @SerializedName(value = "is_default", alternate = ["isDefault"]) val isDefault: Boolean = false,
    @SerializedName(value = "sort_order", alternate = ["sortOrder"]) val sortOrder: Int = 0,
    @SerializedName(value = "created_at", alternate = ["createdAt"]) val createdAt: Long? = null,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"]) val updatedAt: Long? = null,
    @SerializedName(value = "deleted_at", alternate = ["deletedAt"]) val deletedAt: Long? = null
)

/**
 * 同步请求
 */
data class SyncRequest(
    @SerializedName("transactions") val transactions: List<TransactionDto>,
    @SerializedName(value = "last_sync_time", alternate = ["lastSyncTime"]) val lastSyncTime: Long
)

/**
 * 账户同步请求（与后端 AccountSyncRequest 对齐）
 */
data class AccountSyncRequest(
    @SerializedName("accounts") val accounts: List<AccountDto>,
    @SerializedName(value = "last_sync_time", alternate = ["lastSyncTime"]) val lastSyncTime: Long
)

data class SyncConflictDto(
    @SerializedName("transactionId") val transactionId: Long? = null,
    @SerializedName("clientVersion") val clientVersion: TransactionDto? = null,
    @SerializedName("serverVersion") val serverVersion: TransactionDto? = null
)

data class SyncResponseDto(
    @SerializedName("syncedTransactions") val syncedTransactions: List<TransactionDto>? = null,
    @SerializedName("conflicts") val conflicts: List<SyncConflictDto>? = null,
    @SerializedName("serverTime") val serverTime: Long? = null
)
