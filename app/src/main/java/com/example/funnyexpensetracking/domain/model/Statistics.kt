package com.example.funnyexpensetracking.domain.model

/**
 * 统计数据领域模型
 */
data class Statistics(
    val period: StatisticsPeriod,
    val startDate: Long,
    val endDate: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val netIncome: Double,
    val categoryBreakdown: List<CategoryStat>,
    val chartUrl: String? = null
)

/**
 * 统计周期
 */
enum class StatisticsPeriod {
    MONTHLY,
    YEARLY
}

/**
 * 分类统计
 */
data class CategoryStat(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val type: TransactionType
)

/**
 * 趋势统计
 */
data class TrendStatistics(
    val periodStats: List<Statistics>,
    val avgIncome: Double,
    val avgExpense: Double,
    val incomeTrend: TrendDirection,
    val expenseTrend: TrendDirection
)

/**
 * 趋势方向
 */
enum class TrendDirection {
    INCREASING,
    STABLE,
    DECREASING
}

