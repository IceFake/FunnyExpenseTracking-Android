package com.example.funnyexpensetracking.domain.usecase

import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.AssetBaselineDao
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.local.dao.InvestmentDao
import com.example.funnyexpensetracking.data.local.entity.AssetBaselineEntity
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeEntity
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
    val currentAsset: Double,                    // 当前资产值
    val incomePerMinute: Double,                 // 每分钟收入
    val expensePerMinute: Double,                // 每分钟支出
    val netChangePerMinute: Double,              // 每分钟净变动
    val baselineTimestamp: Long,                 // 基准时间
    val baselineAmount: Double,                  // 基准金额
    val totalAccountBalance: Double = 0.0,       // 各账户余额总和
    val totalFixedIncome: Double = 0.0,          // 固定收入累计总额
    val totalFixedExpense: Double = 0.0,         // 固定支出累计总额
    val totalInvestmentValue: Double = 0.0       // 投资/理财当前价值
)

/**
 * 实时资产计算器
 *
 * 总资产计算公式：
 * 总资产 = 各账户金额之和 + 固定收支总额度 + 理财总资产价值
 *
 * 其中固定收支总额度 = 固定收入累计总额 - 固定支出累计总额
 *
 * 触发更新的时机：
 * 1. 账户余额发生变化时
 * 2. 固定收支累计金额发生变化时（每分钟定时更新累计时间）
 * 3. 理财资产价值发生变化时
 */
@Singleton
class RealtimeAssetCalculator @Inject constructor(
    private val fixedIncomeDao: FixedIncomeDao,
    private val accountDao: AccountDao,
    private val assetBaselineDao: AssetBaselineDao,
    private val investmentDao: InvestmentDao
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

    private var fixedIncomeUpdateJob: Job? = null

    init {
        // 监听各数据源的变化
        observeAccountChanges()
        observeFixedIncomeChanges()
        observeInvestmentChanges()

        // 启动固定收支累计时间的定时更新
        startFixedIncomeAccumulationUpdate()

        // 初始计算
        scope.launch {
            recalculateAsset()
        }
    }

    /**
     * 监听账户余额变化
     */
    private fun observeAccountChanges() {
        scope.launch {
            accountDao.getTotalBalanceFlow()
                .distinctUntilChanged()
                .collect {
                    recalculateAsset()
                }
        }
    }

    /**
     * 监听固定收支累计金额变化
     */
    private fun observeFixedIncomeChanges() {
        scope.launch {
            // 合并固定收入和支出的累计总额变化
            combine(
                fixedIncomeDao.getAllActiveFixedIncomes(),
                flowOf(Unit) // 触发初始值
            ) { _, _ -> Unit }
                .collect {
                    recalculateAsset()
                }
        }
    }

    /**
     * 监听投资价值变化
     */
    private fun observeInvestmentChanges() {
        scope.launch {
            investmentDao.getTotalCurrentValueFlow()
                .distinctUntilChanged()
                .collect {
                    recalculateAsset()
                }
        }
    }

    /**
     * 启动固定收支累计时间的定时更新（每分钟更新一次）
     */
    private fun startFixedIncomeAccumulationUpdate() {
        fixedIncomeUpdateJob?.cancel()
        fixedIncomeUpdateJob = scope.launch {
            while (true) {
                delay(60_000) // 1分钟
                updateFixedIncomeAccumulations()
            }
        }
    }

    /**
     * 更新所有固定收支的累计时间
     * 每分钟调用一次
     */
    private suspend fun updateFixedIncomeAccumulations() {
        val currentMinuteTimestamp = getCurrentMinuteTimestamp()
        val fixedIncomes = fixedIncomeDao.getAllFixedIncomesList()

        var hasChanges = false

        for (entity in fixedIncomes) {
            // 检查是否需要更新（当前时间与上次记录时间不同）
            if (entity.lastRecordTime != currentMinuteTimestamp) {
                updateFixedIncomeAccumulation(entity, currentMinuteTimestamp)
                hasChanges = true
            }
        }

        // 如果有变化，重新计算资产
        if (hasChanges) {
            recalculateAsset()
        }
    }

    /**
     * 更新单个固定收支的累计时间
     */
    private suspend fun updateFixedIncomeAccumulation(
        entity: FixedIncomeEntity,
        currentTime: Long
    ) {
        val lastTime = entity.lastRecordTime

        // 如果上次记录时间为0（新条目），初始化为开始时间或当前时间
        val effectiveLastTime = if (lastTime == 0L) {
            maxOf(entity.startDate, currentTime - 60_000) // 最多回溯1分钟
        } else {
            lastTime
        }

        // 计算时间差（分钟）
        val deltaMinutes = (currentTime - effectiveLastTime) / 60_000

        // 如果时间差为负或0，只更新记录时间
        if (deltaMinutes <= 0) {
            fixedIncomeDao.updateAccumulated(
                id = entity.id,
                accumulatedMinutes = entity.accumulatedMinutes,
                accumulatedAmount = entity.accumulatedAmount,
                lastRecordTime = currentTime
            )
            return
        }

        // 计算在生效期间内的有效分钟数
        val effectiveMinutes = calculateEffectiveMinutes(
            entity = entity,
            fromTime = effectiveLastTime,
            toTime = currentTime
        )

        // 更新累计时间
        val newAccumulatedMinutes = entity.accumulatedMinutes + effectiveMinutes

        // 根据累计时间计算累计金额（优先按周期计算以减小误差）
        val newAccumulatedAmount = entity.frequency.calculateAccumulatedAmount(
            entity.amount,
            newAccumulatedMinutes
        )

        // 更新数据库
        fixedIncomeDao.updateAccumulated(
            id = entity.id,
            accumulatedMinutes = newAccumulatedMinutes,
            accumulatedAmount = newAccumulatedAmount,
            lastRecordTime = currentTime
        )
    }

    /**
     * 计算在生效期间内的有效分钟数
     */
    private fun calculateEffectiveMinutes(
        entity: FixedIncomeEntity,
        fromTime: Long,
        toTime: Long
    ): Long {
        // 如果条目未激活，返回0
        if (!entity.isActive) return 0

        // 计算有效的时间范围
        val effectiveStart = maxOf(fromTime, entity.startDate)
        val effectiveEnd = if (entity.endDate != null) {
            minOf(toTime, entity.endDate)
        } else {
            toTime
        }

        // 如果有效开始时间大于等于有效结束时间，返回0
        if (effectiveStart >= effectiveEnd) return 0

        // 返回有效分钟数
        return (effectiveEnd - effectiveStart) / 60_000
    }

    /**
     * 重新计算资产
     *
     * 总资产 = 各账户金额之和 + 固定收支总额度 + 理财总资产价值
     * 固定收支总额度 = 固定收入累计总额 - 固定支出累计总额
     */
    suspend fun recalculateAsset() {
        val currentMinuteTimestamp = getCurrentMinuteTimestamp()

        // 1. 获取各账户余额总和
        val totalAccountBalance = accountDao.getTotalBalance() ?: 0.0

        // 2. 获取固定收支累计总额
        val totalFixedIncome = fixedIncomeDao.getTotalAccumulatedIncome()
        val totalFixedExpense = fixedIncomeDao.getTotalAccumulatedExpense()
        val fixedIncomeNetAmount = totalFixedIncome - totalFixedExpense

        // 3. 获取投资/理财当前价值
        val totalInvestmentValue = investmentDao.getTotalCurrentValue()

        // 计算总资产 = 账户余额总和 + 固定收支净额 + 投资价值
        val currentAsset = totalAccountBalance + fixedIncomeNetAmount + totalInvestmentValue

        // 计算每分钟净变动（用于显示）
        val fixedIncomes = fixedIncomeDao.getAllFixedIncomesList()
        var incomePerMinute = 0.0
        var expensePerMinute = 0.0

        fixedIncomes.forEach { entity ->
            if (entity.isEffectiveAt(currentMinuteTimestamp)) {
                val perMinute = entity.getAmountPerMinute()
                if (entity.type == FixedIncomeType.INCOME) {
                    incomePerMinute += perMinute
                } else {
                    expensePerMinute += perMinute
                }
            }
        }

        val netChangePerMinute = incomePerMinute - expensePerMinute

        // 保存基准值
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
            totalAccountBalance = totalAccountBalance,
            totalFixedIncome = totalFixedIncome,
            totalFixedExpense = totalFixedExpense,
            totalInvestmentValue = totalInvestmentValue
        )
    }

    /**
     * 当普通收支变化时重新计算资产（账户余额已通过Flow监听）
     */
    suspend fun onTransactionChanged() {
        recalculateAsset()
    }

    /**
     * 当账户余额变化时更新（账户余额已通过Flow监听，此方法保留用于兼容）
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
     * 获取当前分钟的时间戳（精确到分钟）
     */
    private fun getCurrentMinuteTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
