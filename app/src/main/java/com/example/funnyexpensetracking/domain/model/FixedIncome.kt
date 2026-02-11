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
     * 优先按完整周期计算，剩余分钟按比例计算，以减小误差
     */
    fun calculateAccumulatedAmount(amount: Double, accumulatedMinutes: Long): Double {
        val minutesPerCycle = getMinutesPerCycle()
        val completeCycles = accumulatedMinutes / minutesPerCycle
        val remainingMinutes = accumulatedMinutes % minutesPerCycle

        // 完整周期的金额 + 剩余分钟按比例的金额
        return (completeCycles * amount) + (remainingMinutes.toDouble() / minutesPerCycle * amount)
    }

    /**
     * 转换为每分钟收益/支出的倍数（兼容旧代码）
     */
    fun toMinuteMultiplier(): Double {
        return 1.0 / getMinutesPerCycle()
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
    val amount: Double,                    // 周期金额
    val type: FixedIncomeType,
    val frequency: FixedIncomeFrequency,
    val startDate: Long,                   // 开始日期（精确至分钟）
    val endDate: Long? = null,             // 结束日期（精确至分钟，可为空表示持续）
    val isActive: Boolean = true,          // 是否生效
    val accumulatedMinutes: Long = 0,      // 累计生效时间（单位：分钟）
    val accumulatedAmount: Double = 0.0,   // 累计收支总额
    val lastRecordTime: Long = 0           // 上次记录时间点
) {
    /**
     * 计算每分钟的收益/支出金额
     */
    fun getAmountPerMinute(): Double {
        return amount * frequency.toMinuteMultiplier()
    }

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
     * 格式化累计时间显示
     */
    fun getFormattedAccumulatedTime(): String {
        val totalMinutes = accumulatedMinutes
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return buildString {
            if (days > 0) append("${days}天")
            if (hours > 0) append("${hours}小时")
            if (minutes > 0 || isEmpty()) append("${minutes}分钟")
        }
    }
}

