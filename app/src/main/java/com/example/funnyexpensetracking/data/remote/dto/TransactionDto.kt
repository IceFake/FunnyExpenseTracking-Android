package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 通用API响应包装（与后端 [ApiResponse] 对齐）
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
)

/**
 * 交易记录 DTO（与后端 TransactionDto 的 JSON 命名一致：camelCase）
 * 
 * 后端 ID 为 UUID 字符串，因此 id 类型为 String?。
 */
data class TransactionDto(
    @SerializedName("id") var id: String? = null,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("category") val category: String,
    @SerializedName(value = "accountId", alternate = ["account_id"]) val accountId: Long? = null,
    @SerializedName("note") val note: String = "",
    @SerializedName("date") val date: Long,
    @SerializedName(value = "createdAt", alternate = ["created_at"]) val createdAt: Long? = null,
    @SerializedName(value = "updatedAt", alternate = ["updated_at"]) val updatedAt: Long? = null,
    @SerializedName("version") val version: Long? = null,
    @SerializedName(value = "deletedAt", alternate = ["deleted_at"]) val deletedAt: Long? = null
)

/**
 * 账户 DTO（与后端 AccountDto 对齐）
 */
data class AccountDto(
    @SerializedName("id") var id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String = "",
    @SerializedName("balance") val balance: Double = 0.0,
    @SerializedName(value = "isDefault", alternate = ["is_default"]) val isDefault: Boolean = false,
    @SerializedName(value = "sortOrder", alternate = ["sort_order"]) val sortOrder: Int = 0,
    @SerializedName(value = "createdAt", alternate = ["created_at"]) val createdAt: Long? = null,
    @SerializedName(value = "updatedAt", alternate = ["updated_at"]) val updatedAt: Long? = null,
    @SerializedName(value = "deletedAt", alternate = ["deleted_at"]) val deletedAt: Long? = null
)

/**
 * 同步请求（与后端 SyncRequest 对齐）
 */
data class SyncRequest(
    @SerializedName("transactions") val transactions: List<TransactionDto>,
    @SerializedName(value = "lastSyncTime", alternate = ["last_sync_time"]) val lastSyncTime: Long
)

/**
 * 账户同步请求（与后端 AccountSyncRequest 对齐）
 */
data class AccountSyncRequest(
    @SerializedName("accounts") val accounts: List<AccountDto>,
    @SerializedName(value = "lastSyncTime", alternate = ["last_sync_time"]) val lastSyncTime: Long
)

/**
 * 同步冲突 DTO（与后端 SyncConflict 对齐）
 */
data class SyncConflictDto(
    @SerializedName("transactionId") val transactionId: String? = null,
    @SerializedName("clientVersion") val clientVersion: TransactionDto? = null,
    @SerializedName("serverVersion") val serverVersion: TransactionDto? = null
)

/**
 * 同步响应 DTO（与后端 SyncResponse 对齐）
 */
data class SyncResponseDto(
    @SerializedName("syncedTransactions") val syncedTransactions: List<TransactionDto>? = null,
    @SerializedName("conflicts") val conflicts: List<SyncConflictDto>? = null,
    @SerializedName("serverTime") val serverTime: Long? = null
)
