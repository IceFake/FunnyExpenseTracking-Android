package com.example.funnyexpensetracking.data.local.entity
)
    val createdAt: Long = System.currentTimeMillis() // 创建时间
    val date: Long,                        // 交易日期（时间戳）
    val note: String = "",                 // 备注
    val category: String,                  // 分类（如：餐饮、交通、工资等）
    val type: TransactionType,             // 类型：收入/支出
    val amount: Double,                    // 金额
    val id: Long = 0,
    @PrimaryKey(autoGenerate = true)
data class TransactionEntity(
@Entity(tableName = "transactions")
 */
 * 交易记录实体
/**

}
    EXPENSE     // 支出
    INCOME,     // 收入
enum class TransactionType {
 */
 * 交易类型枚举
/**

import androidx.room.PrimaryKey
import androidx.room.Entity


