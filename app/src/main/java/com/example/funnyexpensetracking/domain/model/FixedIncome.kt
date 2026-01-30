package com.example.funnyexpensetracking.domain.model

/**
 * 固定收支频率
 */
enum class FixedIncomeFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * 转换为每分钟收益/支出的倍数
     */
    fun toMinuteMultiplier(): Double {
        return when (this) {
            DAILY -> 1.0 / (24 * 60)
            WEEKLY -> 1.0 / (7 * 24 * 60)
            MONTHLY -> 1.0 / (30 * 24 * 60)
            YEARLY -> 1.0 / (365 * 24 * 60)
        }
    }
}

/**
 * 固定收支类型
 */
enum class FixedIncomeType {
    INCOME,
    EXPENSE
}

/**
 * 固定收支领域模型
 */
data class FixedIncome(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: FixedIncomeType,
    val frequency: FixedIncomeFrequency,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val accumulatedAmount: Double = 0.0  // 累计收入/支出总额
) {
    /**
     * 计算每分钟的收益/支出金额
     */
    fun getAmountPerMinute(): Double {
        return amount * frequency.toMinuteMultiplier()
    }
}

