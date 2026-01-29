package com.example.funnyexpensetracking.domain.model

/**
 * 股票持仓领域模型
 */
data class StockHolding(
    val id: Long = 0,
    val symbol: String,
    val name: String,
    val shares: Double,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val currentPrice: Double = purchasePrice,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * 计算当前市值
     */
    val marketValue: Double
        get() = shares * currentPrice

    /**
     * 计算成本
     */
    val cost: Double
        get() = shares * purchasePrice

    /**
     * 计算盈亏金额
     */
    val profitLoss: Double
        get() = marketValue - cost

    /**
     * 计算盈亏百分比
     */
    val profitLossPercent: Double
        get() = if (cost > 0) (profitLoss / cost) * 100 else 0.0
}

/**
 * 股票行情
 */
data class StockQuote(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val closePrice: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val timestamp: Long
)

