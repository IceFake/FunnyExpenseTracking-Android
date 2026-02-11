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
    YEARLY;     // 每年

    /**
     * 获取该频率对应的分钟数
     */
    fun getMinutesPerCycle(): Long {
        return when (this) {
            DAILY -> 24L * 60           // 1440 分钟
            WEEKLY -> 7L * 24 * 60      // 10080 分钟
            MONTHLY -> 30L * 24 * 60    // 43200 分钟
            YEARLY -> 365L * 24 * 60    // 525600 分钟
        }
    }

    /**
     * 根据累计生效分钟数计算累计金额
     * 优先按完整周期计算，剩余分钟按比例计算
     */
    fun calculateAccumulatedAmount(amount: Double, accumulatedMinutes: Long): Double {
        val minutesPerCycle = getMinutesPerCycle()
        val completeCycles = accumulatedMinutes / minutesPerCycle
        val remainingMinutes = accumulatedMinutes % minutesPerCycle

        // 完整周期的金额 + 剩余分钟按比例的金额
        return (completeCycles * amount) + (remainingMinutes.toDouble() / minutesPerCycle * amount)
    }
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
 *
 * 计算逻辑：
 * 1. 每次时间更新时，检查是否处于生效期间（startDate <= 当前时间 <= endDate，且 isActive = true）
 * 2. 如果处于生效期间，计算当前时间与上次记录时间的差值（分钟），累加到 accumulatedMinutes
 * 3. 更新 lastRecordTime 为当前时间
 * 4. 根据 accumulatedMinutes 和 frequency 计算 accumulatedAmount
 */
@Entity(tableName = "fixed_incomes")
data class FixedIncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                      // 名称（如：工资、房租）
    val amount: Double,                    // 周期金额
    val type: FixedIncomeType,             // 类型：收入/支出
    val frequency: FixedIncomeFrequency,   // 频率：DAILY/WEEKLY/MONTHLY/YEARLY
    val startDate: Long,                   // 开始日期（精确至分钟）
    val endDate: Long? = null,             // 结束日期（精确至分钟，可为空表示持续）
    val isActive: Boolean = true,          // 是否生效（用户可手动停用）
    val accumulatedMinutes: Long = 0,      // 累计生效时间（单位：分钟）
    val accumulatedAmount: Double = 0.0,   // 累计收支总额（由累计生效时间计算得来）
    val lastRecordTime: Long = 0,          // 上次记录时间点（精确至分钟）
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 检查在指定时间点是否处于生效状态
     */
    fun isEffectiveAt(timestamp: Long): Boolean {
        if (!isActive) return false
        if (timestamp < startDate) return false
        if (endDate != null && timestamp > endDate) return false
        return true
    }

    /**
     * 计算每分钟的金额
     */
    fun getAmountPerMinute(): Double {
        return amount / frequency.getMinutesPerCycle()
    }
}

