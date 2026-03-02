package com.example.funnyexpensetracking.domain.usecase.transaction

import com.example.funnyexpensetracking.domain.model.Account
import com.example.funnyexpensetracking.domain.model.DailyTransactions
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency
import com.example.funnyexpensetracking.domain.model.FixedIncomeType
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.domain.repository.AccountRepository
import com.example.funnyexpensetracking.domain.repository.AssetRepository
import com.example.funnyexpensetracking.domain.repository.TransactionRepository
import com.example.funnyexpensetracking.domain.usecase.RealtimeAssetCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易记录相关业务逻辑
 */
@Singleton
class TransactionUseCases @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val realtimeAssetCalculator: RealtimeAssetCalculator
) {

    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    }
    private val dayOfWeekFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEEE", Locale.CHINA)
    }



    /**
     * 交易数据加载结果
     */
    data class TransactionLoadResult(
        val transactions: List<Transaction>,
        val accounts: List<Account>,
        val dailyTransactions: List<DailyTransactions>,
        val todayIncome: Double,
        val todayExpense: Double,
        val totalBalance: Double
    )

    /**
     * 加载指定日期范围的交易数据
     */
    fun loadTransactionsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<TransactionLoadResult> {
        return combine(
            transactionRepository.getTransactionsByDateRange(startDate, endDate),
            accountRepository.getAllAccounts()
        ) { transactions, accounts ->
            // 创建账户映射以便查找账户名称
            val accountMap = accounts.associateBy { it.id }
            
            // 为交易记录设置账户名称
            val transactionsWithAccountNames = transactions.map { transaction ->
                val accountName = accountMap[transaction.accountId]?.name ?: "未知账户"
                transaction.copy(accountName = accountName)
            }

            // 计算今日收支
            val todayIncome = transactionsWithAccountNames.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val todayExpense = transactionsWithAccountNames.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            // 计算总余额
            val totalBalance = accounts.sumOf { it.balance }

            // 按日期分组
            val dailyTransactions = groupTransactionsByDate(transactionsWithAccountNames)

            TransactionLoadResult(
                transactions = transactionsWithAccountNames,
                accounts = accounts,
                dailyTransactions = dailyTransactions,
                todayIncome = todayIncome,
                todayExpense = todayExpense,
                totalBalance = totalBalance
            )
        }
    }

    /**
     * 按日期分组交易记录
     */
    private fun groupTransactionsByDate(transactions: List<Transaction>): List<DailyTransactions> {
        return transactions
            .groupBy { getDateOnly(it.date) }
            .map { (dateTimestamp, transactionList) ->
                DailyTransactions(
                    date = dateTimestamp,
                    dateString = dateFormat.get().format(Date(dateTimestamp)),
                    dayOfWeek = dayOfWeekFormat.get().format(Date(dateTimestamp)),
                    totalIncome = transactionList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    totalExpense = transactionList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    transactions = transactionList.sortedByDescending { it.createdAt }
                )
            }
            .sortedByDescending { it.date }
    }

    /**
     * 获取日期的0点时间戳
     */
    private fun getDateOnly(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 添加交易记录
     */
    suspend fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        accountId: Long,
        note: String = "",
        date: Long = System.currentTimeMillis()
    ): Long {
        // 创建交易记录
        val transaction = Transaction(
            amount = amount,
            type = type,
            category = category,
            accountId = accountId,
            note = note,
            date = date
        )
        val transactionId = transactionRepository.addTransaction(transaction)

        // 更新账户余额
        val balanceChange = if (type == TransactionType.INCOME) amount else -amount
        accountRepository.updateBalance(accountId, balanceChange)

        // 通知资产计算器普通收支发生变化
        realtimeAssetCalculator.onTransactionChanged()

        return transactionId
    }

    /**
     * 删除交易记录
     */
    suspend fun deleteTransaction(transaction: Transaction) {
        // 获取交易记录（确保存在）
        val existing = transactionRepository.getTransactionById(transaction.id)
        if (existing == null) {
            throw IllegalArgumentException("交易记录不存在")
        }

        // 删除交易记录（软删除或硬删除由repository处理）
        transactionRepository.deleteTransaction(transaction)

        // 回滚账户余额
        val balanceChange = if (transaction.type == TransactionType.INCOME) -transaction.amount else transaction.amount
        accountRepository.updateBalance(transaction.accountId, balanceChange)

        // 通知资产计算器普通收支发生变化
        realtimeAssetCalculator.onTransactionChanged()
    }

    /**
     * 更新交易记录
     */
    suspend fun updateTransaction(
        id: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        accountId: Long,
        note: String,
        date: Long
    ) {
        val existing = transactionRepository.getTransactionById(id)
        if (existing == null) {
            throw IllegalArgumentException("交易记录不存在")
        }

        // 回滚旧账户余额
        val oldBalanceChange = if (existing.type == TransactionType.INCOME) -existing.amount else existing.amount
        accountRepository.updateBalance(existing.accountId, oldBalanceChange)

        // 更新交易记录
        val updatedTransaction = Transaction(
            id = id,
            amount = amount,
            type = type,
            category = category,
            accountId = accountId,
            note = note,
            date = date,
            createdAt = existing.createdAt
        )
        transactionRepository.updateTransaction(updatedTransaction)

        // 更新新账户余额
        val newBalanceChange = if (type == TransactionType.INCOME) amount else -amount
        accountRepository.updateBalance(accountId, newBalanceChange)

        // 通知资产计算器普通收支发生变化
        realtimeAssetCalculator.onTransactionChanged()
    }

    /**
     * 添加账户
     */
    suspend fun addAccount(
        name: String,
        initialBalance: Double = 0.0,
        icon: String = "",
        isDefault: Boolean = false,
        sortOrder: Int = 0
    ): Long {
        val account = Account(
            name = name,
            icon = icon,
            balance = initialBalance,
            isDefault = isDefault,
            sortOrder = sortOrder
        )
        return accountRepository.addAccount(account)
    }

    /**
     * 初始化默认账户（如果账户列表为空）
     */
    suspend fun initializeDefaultAccountsIfEmpty() {
        val accounts = accountRepository.getAllAccounts().first()
        if (accounts.isEmpty()) {
            val defaultAccounts = listOf(
                Account(name = "现金", icon = "cash", balance = 0.0, isDefault = true, sortOrder = 0),
                Account(name = "微信", icon = "wechat", balance = 0.0, isDefault = false, sortOrder = 1),
                Account(name = "支付宝", icon = "alipay", balance = 0.0, isDefault = false, sortOrder = 2),
                Account(name = "银行卡", icon = "bank", balance = 0.0, isDefault = false, sortOrder = 3)
            )
            defaultAccounts.forEach { accountRepository.addAccount(it) }
        }
    }

    /**
     * 删除账户
     */
    suspend fun deleteAccount(account: Account) {
        accountRepository.deleteAccount(account)
        realtimeAssetCalculator.recalculateAsset()
    }

    /**
     * 更新账户（名称和余额）
     */
    suspend fun updateAccount(accountId: Long, name: String, balance: Double) {
        val existing = accountRepository.getAccountById(accountId)
        if (existing != null) {
            val updatedAccount = existing.copy(name = name, balance = balance)
            accountRepository.updateAccount(updatedAccount)
            realtimeAssetCalculator.recalculateAsset()
        }
    }

    /**
     * 直接设置账户余额
     */
    suspend fun setAccountBalance(accountId: Long, balance: Double) {
        accountRepository.updateBalance(accountId, balance)
        realtimeAssetCalculator.recalculateAsset()
    }

    /**
     * 添加固定收支
     */
    suspend fun addFixedIncome(
        name: String,
        amount: Double,
        type: FixedIncomeType,
        frequency: FixedIncomeFrequency,
        startDate: Long,
        endDate: Long? = null
    ): Long {
        val fixedIncome = FixedIncome(
            name = name,
            amount = amount,
            type = type,
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            isActive = true,
            accumulatedMinutes = 0,
            accumulatedAmount = 0.0,
            lastRecordTime = startDate
        )
        val id = assetRepository.addFixedIncome(fixedIncome)
        realtimeAssetCalculator.recalculateAsset()
        return id
    }
}