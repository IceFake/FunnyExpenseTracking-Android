package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 交易类型枚举
 */
enum class TransactionType {
    INCOME,     // 收入
    EXPENSE     // 支出
}

/**
 * 同步状态枚举
 */
enum class SyncStatus {
    SYNCED,         // 已同步
    PENDING_UPLOAD, // 等待上传（新增或修改）
    PENDING_DELETE, // 等待删除
    CONFLICT        // 冲突（本地和服务器都有修改）
}

/**
 * 交易记录实体
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long? = null,            // 服务器端ID（用于同步）
    val amount: Double,                    // 金额
    val type: TransactionType,             // 类型：收入/支出
    val category: String,                  // 分类（如：餐饮、交通、工资等）
    val accountId: Long,                   // 账户ID
    val note: String = "",                 // 备注
    val date: Long,                        // 交易日期（时间戳）
    val createdAt: Long = System.currentTimeMillis(), // 创建时间
    val updatedAt: Long = System.currentTimeMillis(), // 更新时间
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD, // 同步状态
    val lastSyncAt: Long? = null           // 上次同步时间
)


