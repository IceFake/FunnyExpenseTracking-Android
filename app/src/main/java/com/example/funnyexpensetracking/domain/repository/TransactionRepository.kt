package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * 交易记录Repository接口
 */
interface TransactionRepository {

    /**
     * 获取所有交易记录
     */
    fun getAllTransactions(): Flow<List<Transaction>>

    /**
     * 根据日期范围获取交易记录
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    /**
     * 根据类型获取交易记录
     */
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    /**
     * 根据分类获取交易记录
     */
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    /**
     * 根据ID获取交易记录
     */
    suspend fun getTransactionById(id: Long): Transaction?

    /**
     * 添加交易记录
     */
    suspend fun addTransaction(transaction: Transaction): Long

    /**
     * 更新交易记录
     */
    suspend fun updateTransaction(transaction: Transaction)

    /**
     * 删除交易记录
     */
    suspend fun deleteTransaction(transaction: Transaction)

    /**
     * 获取指定类型和日期范围的总金额
     */
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Double

    /**
     * 获取所有分类
     */
    suspend fun getCategoriesByType(type: TransactionType): List<String>

    /**
     * 同步数据到服务器
     */
    suspend fun syncWithServer()
}

