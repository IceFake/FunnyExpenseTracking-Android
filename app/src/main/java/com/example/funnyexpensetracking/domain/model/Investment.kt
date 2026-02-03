package com.example.funnyexpensetracking.domain.model

/**
 * 投资分类
 */
enum class InvestmentCategory {
    STOCK,      // 股票
    OTHER       // 其他
}

/**
 * 投资条目领域模型
 */
data class Investment(
    val id: Long = 0,
    val category: InvestmentCategory,
    val description: String,           // 股票代码或其他描述
    val quantity: Double = 0.0,        // 数量（仅股票）
    val investment: Double,            // 投入金额
    val currentPrice: Double = 0.0,    // 当前单价（仅股票）
    val currentValue: Double = 0.0,    // 当前价值（其他类型可手动编辑）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 计算当前价值
     */
    fun calcCurrentValue(): Double {
        return when (category) {
            InvestmentCategory.STOCK -> {
                if (currentPrice > 0) currentPrice * quantity else investment
            }
            InvestmentCategory.OTHER -> {
                if (currentValue > 0) currentValue else investment
            }
        }
    }

    /**
     * 计算盈亏
     */
    fun getProfitLoss(): Double {
        return calcCurrentValue() - investment
    }

    /**
     * 盈亏百分比
     */
    fun getProfitLossPercent(): Double {
        return if (investment > 0) {
            (getProfitLoss() / investment) * 100
        } else 0.0
    }
}

