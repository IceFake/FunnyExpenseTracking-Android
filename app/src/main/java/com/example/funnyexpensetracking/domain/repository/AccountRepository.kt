package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * 账户Repository接口
 */
interface AccountRepository {

    /**
     * 获取所有账户
     */
    fun getAllAccounts(): Flow<List<Account>>

    /**
     * 根据ID获取账户
     */
    suspend fun getAccountById(id: Long): Account?

    /**
     * 获取默认账户
     */
    suspend fun getDefaultAccount(): Account?

    /**
     * 添加账户
     */
    suspend fun addAccount(account: Account): Long

    /**
     * 更新账户
     */
    suspend fun updateAccount(account: Account)

    /**
     * 删除账户
     */
    suspend fun deleteAccount(account: Account)

    /**
     * 更新账户余额
     */
    suspend fun updateBalance(accountId: Long, amount: Double)

    /**
     * 设置默认账户
     */
    suspend fun setDefaultAccount(accountId: Long)

    /**
     * 获取总余额
     */
    suspend fun getTotalBalance(): Double

    /**
     * 与服务器同步
     */
    suspend fun syncWithServer()
}

