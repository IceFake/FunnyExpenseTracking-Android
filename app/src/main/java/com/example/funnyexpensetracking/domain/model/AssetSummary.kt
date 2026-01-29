package com.example.funnyexpensetracking.domain.model

/**
 * 资产汇总领域模型
 */
data class AssetSummary(
    val totalAsset: Double,               // 总资产
    val cashAsset: Double,                // 现金资产（基于记账+固定收支计算）
    val stockAsset: Double,               // 股票资产市值
    val totalIncome: Double,              // 累计收入
    val totalExpense: Double,             // 累计支出
    val fixedIncomePerMinute: Double,     // 每分钟固定收入
    val fixedExpensePerMinute: Double,    // 每分钟固定支出
    val netIncomePerMinute: Double,       // 每分钟净收入
    val stockProfitLoss: Double,          // 股票盈亏
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 资产快照
 */
data class AssetSnapshot(
    val id: Long = 0,
    val totalAsset: Double,
    val cashAsset: Double,
    val stockAsset: Double,
    val timestamp: Long
)

/**
 * 资产变化趋势
 */
data class AssetTrend(
    val snapshots: List<AssetSnapshot>,
    val startTime: Long,
    val endTime: Long,
    val changeAmount: Double,
    val changePercent: Double
)

