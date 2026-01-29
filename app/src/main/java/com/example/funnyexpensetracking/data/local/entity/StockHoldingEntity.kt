package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 股票持仓实体
 */
@Entity(tableName = "stock_holdings")
data class StockHoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,                    // 股票代码
    val name: String,                      // 股票名称
    val shares: Double,                    // 持有股数
    val purchasePrice: Double,             // 购入价格
    val purchaseDate: Long,                // 购入日期
    val currentPrice: Double = purchasePrice,  // 当前价格（缓存）
    val lastUpdated: Long = System.currentTimeMillis(), // 最后更新时间
    val createdAt: Long = System.currentTimeMillis()
)

