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
 * 交易记录实体
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,                    // 金额
    val type: TransactionType,             // 类型：收入/支出
    val category: String,                  // 分类（如：餐饮、交通、工资等）
    val accountId: Long,                   // 账户ID
    val note: String = "",                 // 备注
    val date: Long,                        // 交易日期（时间戳）
    val createdAt: Long = System.currentTimeMillis() // 创建时间
)


