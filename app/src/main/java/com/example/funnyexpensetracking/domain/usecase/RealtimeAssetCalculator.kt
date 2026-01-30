package com.example.funnyexpensetracking.domain.usecase

import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.AssetBaselineDao
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.AssetBaselineEntity
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实时资产数据
 */
data class RealtimeAssetData(
    val currentAsset: Double,           // 当前资产值
    val incomePerMinute: Double,        // 每分钟收入
    val expensePerMinute: Double,       // 每分钟支出
    val netChangePerMinute: Double,     // 每分钟净变动
    val baselineTimestamp: Long,        // 基准时间
    val baselineAmount: Double,         // 基准金额
    val totalTransactionIncome: Double = 0.0,    // 普通收入总额
    val totalTransactionExpense: Double = 0.0,   // 普通支出总额
    val totalFixedIncome: Double = 0.0,          // 固定收入累计总额
    val totalFixedExpense: Double = 0.0          // 固定支出累计总额
)

/**
 * 实时资产计算器
 * 负责根据普通收支和固定收支的累计值计算实时资产
 *
 * 总资产 = 普通收入总额 - 普通支出总额 + 固定收入累计总额 - 固定支出累计总额
 */
@Singleton
class RealtimeAssetCalculator @Inject constructor(
    private val fixedIncomeDao: FixedIncomeDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val assetBaselineDao: AssetBaselineDao
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // 实时资产数据流
    private val _realtimeAsset = MutableStateFlow(RealtimeAssetData(
        currentAsset = 0.0,
        incomePerMinute = 0.0,
        expensePerMinute = 0.0,
        netChangePerMinute = 0.0,
        baselineTimestamp = 0L,
        baselineAmount = 0.0
    ))
    val realtimeAsset: StateFlow<RealtimeAssetData> = _realtimeAsset.asStateFlow()

    private var updateJob: Job? = null

    init {
        startRealtimeUpdate()
    }

    /**
     * 启动实时更新（每分钟更新一次）
     */
    private fun startRealtimeUpdate() {
        updateJob?.cancel()
        updateJob = scope.launch {
            // 初始计算
            recalculateAsset()

            // 每分钟更新一次
            while (true) {
                delay(60_000) // 1分钟
                updateCurrentAsset()
            }
        }
    }

    /**
     * 更新当前资产值
     * 同时自动累加每个固定收支项目的累计金额
     */
    private suspend fun updateCurrentAsset() {
        val fixedIncomes = fixedIncomeDao.getAllActiveFixedIncomes().first()

        // 计算每分钟净变动
        var incomePerMinute = 0.0
        var expensePerMinute = 0.0

        fixedIncomes.forEach { entity ->
            val perMinute = calculatePerMinute(entity.amount, entity.frequency)
            if (entity.type == FixedIncomeType.INCOME) {
                incomePerMinute += perMinute
            } else {
                expensePerMinute += perMinute
            }

            // 自动累加每分钟的变化值到累计金额
            fixedIncomeDao.addToAccumulatedAmount(entity.id, perMinute)
        }

        val netChangePerMinute = incomePerMinute - expensePerMinute

        // 获取普通收支总额
        val totalTransactionIncome = transactionDao.getTotalIncome()
        val totalTransactionExpense = transactionDao.getTotalExpense()

        // 获取固定收支累计总额
        val totalFixedIncome = fixedIncomeDao.getTotalAccumulatedIncome()
        val totalFixedExpense = fixedIncomeDao.getTotalAccumulatedExpense()

        // 计算当前资产 = 普通收入 - 普通支出 + 固定收入累计 - 固定支出累计
        val currentAsset = totalTransactionIncome - totalTransactionExpense +
                          totalFixedIncome - totalFixedExpense

        val currentMinuteTimestamp = getCurrentMinuteTimestamp()

        _realtimeAsset.value = RealtimeAssetData(
            currentAsset = currentAsset,
            incomePerMinute = incomePerMinute,
            expensePerMinute = expensePerMinute,
            netChangePerMinute = netChangePerMinute,
            baselineTimestamp = currentMinuteTimestamp,
            baselineAmount = currentAsset,
            totalTransactionIncome = totalTransactionIncome,
            totalTransactionExpense = totalTransactionExpense,
            totalFixedIncome = totalFixedIncome,
            totalFixedExpense = totalFixedExpense
        )

        // 更新基准值
        val newBaseline = AssetBaselineEntity(
            id = 1,
            baselineTimestamp = currentMinuteTimestamp,
            baselineAmount = currentAsset
        )
        assetBaselineDao.insert(newBaseline)
    }

    /**
     * 重新计算资产（当收支变化时调用）
     * 总资产 = 普通收入总额 - 普通支出总额 + 固定收入累计总额 - 固定支出累计总额
     */
    suspend fun recalculateAsset() {
        val fixedIncomes = fixedIncomeDao.getAllActiveFixedIncomes().first()

        // 计算每分钟净变动
        var incomePerMinute = 0.0
        var expensePerMinute = 0.0

        fixedIncomes.forEach { entity ->
            val perMinute = calculatePerMinute(entity.amount, entity.frequency)
            if (entity.type == FixedIncomeType.INCOME) {
                incomePerMinute += perMinute
            } else {
                expensePerMinute += perMinute
            }
        }

        val netChangePerMinute = incomePerMinute - expensePerMinute

        // 获取普通收支总额
        val totalTransactionIncome = transactionDao.getTotalIncome()
        val totalTransactionExpense = transactionDao.getTotalExpense()

        // 获取固定收支累计总额
        val totalFixedIncome = fixedIncomeDao.getTotalAccumulatedIncome()
        val totalFixedExpense = fixedIncomeDao.getTotalAccumulatedExpense()

        // 计算当前资产 = 普通收入 - 普通支出 + 固定收入累计 - 固定支出累计
        val currentAsset = totalTransactionIncome - totalTransactionExpense +
                          totalFixedIncome - totalFixedExpense

        val currentMinuteTimestamp = getCurrentMinuteTimestamp()

        // 保存新的基准值
        val newBaseline = AssetBaselineEntity(
            id = 1,
            baselineTimestamp = currentMinuteTimestamp,
            baselineAmount = currentAsset
        )
        assetBaselineDao.insert(newBaseline)

        // 更新状态
        _realtimeAsset.value = RealtimeAssetData(
            currentAsset = currentAsset,
            incomePerMinute = incomePerMinute,
            expensePerMinute = expensePerMinute,
            netChangePerMinute = netChangePerMinute,
            baselineTimestamp = currentMinuteTimestamp,
            baselineAmount = currentAsset,
            totalTransactionIncome = totalTransactionIncome,
            totalTransactionExpense = totalTransactionExpense,
            totalFixedIncome = totalFixedIncome,
            totalFixedExpense = totalFixedExpense
        )
    }

    /**
     * 当普通收支变化时重新计算资产
     */
    suspend fun onTransactionChanged() {
        recalculateAsset()
    }

    /**
     * 当账户余额变化时更新（兼容旧接口，现在直接重新计算）
     */
    suspend fun onAccountBalanceChanged(amountChange: Double) {
        recalculateAsset()
    }

    /**
     * 当固定收支的累计金额变化时重新计算资产
     */
    suspend fun onAccumulatedAmountChanged(
        oldAccumulatedAmount: Double,
        newAccumulatedAmount: Double,
        isIncome: Boolean
    ) {
        recalculateAsset()
    }

    /**
     * 计算每分钟金额
     */
    private fun calculatePerMinute(amount: Double, frequency: com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency): Double {
        return when (frequency) {
            com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.DAILY -> amount / (24 * 60)
            com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.WEEKLY -> amount / (7 * 24 * 60)
            com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.MONTHLY -> amount / (30 * 24 * 60)
            com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.YEARLY -> amount / (365 * 24 * 60)
        }
    }

    /**
     * 获取当前分钟的时间戳（精确到分钟）
     */
    private fun getCurrentMinuteTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
