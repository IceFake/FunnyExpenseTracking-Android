package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 投资分类枚举
 */
enum class InvestmentCategory {
    STOCK,      // 股票
    OTHER       // 其他
}

/**
 * 投资条目实体
 */
@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: InvestmentCategory,      // 分类：股票/其他
    val description: String,               // 描述：股票代码或其他描述
    val quantity: Double = 0.0,            // 数量（仅股票使用）
    val investment: Double,                // 投入金额
    val currentPrice: Double = 0.0,        // 当前单价（仅股票使用，从API获取）
    val currentValue: Double = 0.0,        // 当前价值（其他类型可手动编辑）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 计算当前价值
     * 股票：当前价格 * 数量（如果当前价格为0则使用投入金额）
     * 其他：使用手动设置的currentValue，如果为0则使用投入金额
     */
    @androidx.room.Ignore
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
    @androidx.room.Ignore
    fun getProfitLoss(): Double {
        return calcCurrentValue() - investment
    }
}

