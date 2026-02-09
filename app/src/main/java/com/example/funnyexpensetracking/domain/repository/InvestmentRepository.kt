package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.Investment
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * 投资Repository接口
 */
interface InvestmentRepository {

    /**
     * 获取所有投资条目
     */
    fun getAllInvestments(): Flow<List<Investment>>

    /**
     * 根据ID获取投资
     */
    suspend fun getInvestmentById(id: Long): Investment?

    /**
     * 添加投资条目
     */
    suspend fun addInvestment(investment: Investment): Long

    /**
     * 更新投资条目
     */
    suspend fun updateInvestment(investment: Investment)

    /**
     * 删除投资条目
     */
    suspend fun deleteInvestment(id: Long)

    /**
     * 获取所有股票代码
     */
    suspend fun getAllStockCodes(): List<String>

    /**
     * 更新股票当前价格
     */
    suspend fun updateStockPrice(stockCode: String, price: Double)

    /**
     * 刷新所有股票价格（从Yahoo Finance API获取）
     */
    suspend fun refreshAllStockPrices(): Resource<Unit>

    /**
     * 获取股票总市值
     */
    suspend fun getTotalStockValue(): Double

    /**
     * 获取股票总投入
     */
    suspend fun getTotalStockInvestment(): Double
}

