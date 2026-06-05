package com.example.funnyexpensetracking.domain.model

/**
 * 数据来源标记 —— 用于三层兜底方案
 */
enum class DataSource {
    REMOTE,   // 来自后端API（最新数据）
    LOCAL,    // 来自本地Room数据库（离线但完整）
    CACHED    // 来自快照缓存（可能过期，最后兜底）
}

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
    val dailyTrends: List<DailyTrend> = emptyList(),
    val chartUrl: String? = null,
    val dataSource: DataSource = DataSource.REMOTE
)

/**
 * 每日趋势
 */
data class DailyTrend(
    val day: Int,
    val income: Double,
    val expense: Double
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
    val type: TransactionType,
    val dataSource: DataSource = DataSource.REMOTE
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
