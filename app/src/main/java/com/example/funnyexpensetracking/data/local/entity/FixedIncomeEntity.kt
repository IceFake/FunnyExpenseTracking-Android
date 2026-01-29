package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 固定收支频率枚举
 */
enum class FixedIncomeFrequency {
    DAILY,      // 每日
    WEEKLY,     // 每周
    MONTHLY,    // 每月
    YEARLY      // 每年
}

/**
 * 固定收支类型
 */
enum class FixedIncomeType {
    INCOME,     // 固定收入
    EXPENSE     // 固定支出
}

/**
 * 固定收支实体（月工资、房租等固定收支）
 */
@Entity(tableName = "fixed_incomes")
data class FixedIncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                      // 名称（如：工资、房租）
    val amount: Double,                    // 金额
    val type: FixedIncomeType,             // 类型：收入/支出
    val frequency: FixedIncomeFrequency,   // 频率：每月/每年
    val startDate: Long,                   // 开始日期
    val endDate: Long? = null,             // 结束日期（可为空，表示持续）
    val isActive: Boolean = true,          // 是否生效
    val createdAt: Long = System.currentTimeMillis()
)

