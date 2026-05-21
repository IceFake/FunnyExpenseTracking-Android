package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.StockHolding
import com.example.funnyexpensetracking.domain.model.StockQuote
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * 股票Repository接口
 */
interface StockRepository {

    // ========== 持仓管理 ==========

    /**
     * 获取所有股票持仓
     */
    fun getAllStockHoldings(): Flow<List<StockHolding>>

    /**
     * 根据ID获取持仓
     */
    suspend fun getStockHoldingById(id: Long): StockHolding?

    /**
     * 根据股票代码获取持仓
     */
    suspend fun getStockHoldingBySymbol(symbol: String): StockHolding?

    /**
     * 添加股票持仓
     */
    suspend fun addStockHolding(stock: StockHolding): Long

    /**
     * 更新股票持仓
     */
    suspend fun updateStockHolding(stock: StockHolding)

    /**
     * 删除股票持仓
     */
    suspend fun deleteStockHolding(stock: StockHolding)

    /**
     * 获取股票总市值
     */
    suspend fun getTotalStockValue(): Double

    /**
     * 获取股票总成本
     */
    suspend fun getTotalStockCost(): Double

    // ========== 行情数据 ==========

    /**
     * 批量获取股票行情
     */
    suspend fun getBatchQuotes(symbols: List<String>): Resource<List<StockQuote>>

    /**
     * 刷新所有持仓的实时价格
     */
    suspend fun refreshAllStockPrices(): Resource<Unit>
}
