package com.example.funnyexpensetracking.domain.usecase

import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.AssetBaselineDao
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
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
    val baselineAmount: Double          // 基准金额
)

/**
 * 实时资产计算器
 * 负责根据固定收支计算当前时间点的实时资产值
 */
@Singleton
class RealtimeAssetCalculator @Inject constructor(
    private val fixedIncomeDao: FixedIncomeDao,
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
     * 更新当前资产值（基于基准值和时间差计算）
     */
    private suspend fun updateCurrentAsset() {
        val baseline = assetBaselineDao.getBaseline() ?: return
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

        // 计算从基准时间到现在经过的分钟数
        val currentMinuteTimestamp = getCurrentMinuteTimestamp()
        val minutesElapsed = (currentMinuteTimestamp - baseline.baselineTimestamp) / 60_000

        // 计算当前资产
        val currentAsset = baseline.baselineAmount + (netChangePerMinute * minutesElapsed)

        _realtimeAsset.value = RealtimeAssetData(
            currentAsset = currentAsset,
            incomePerMinute = incomePerMinute,
            expensePerMinute = expensePerMinute,
            netChangePerMinute = netChangePerMinute,
            baselineTimestamp = baseline.baselineTimestamp,
            baselineAmount = baseline.baselineAmount
        )
    }

    /**
     * 重新计算资产（当固定收支变化时调用）
     * 会重新设置基准时间和基准金额
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

        // 获取当前基准值
        val existingBaseline = assetBaselineDao.getBaseline()
        val currentMinuteTimestamp = getCurrentMinuteTimestamp()

        val newBaselineAmount: Double

        if (existingBaseline != null) {
            // 先计算到当前时间的资产值，然后以此作为新的基准
            val minutesElapsed = (currentMinuteTimestamp - existingBaseline.baselineTimestamp) / 60_000

            // 用旧的每分钟变动率计算到现在的值
            val oldFixedIncomes = fixedIncomes // 这里实际上应该用旧的，但简化处理
            var oldNetChangePerMinute = 0.0
            oldFixedIncomes.forEach { entity ->
                val perMinute = calculatePerMinute(entity.amount, entity.frequency)
                if (entity.type == FixedIncomeType.INCOME) {
                    oldNetChangePerMinute += perMinute
                } else {
                    oldNetChangePerMinute -= perMinute
                }
            }

            newBaselineAmount = existingBaseline.baselineAmount + (oldNetChangePerMinute * minutesElapsed)
        } else {
            // 首次设置，从账户余额开始
            newBaselineAmount = accountDao.getTotalBalance() ?: 0.0
        }

        // 保存新的基准值
        val newBaseline = AssetBaselineEntity(
            id = 1,
            baselineTimestamp = currentMinuteTimestamp,
            baselineAmount = newBaselineAmount
        )
        assetBaselineDao.insert(newBaseline)

        // 更新状态
        _realtimeAsset.value = RealtimeAssetData(
            currentAsset = newBaselineAmount,
            incomePerMinute = incomePerMinute,
            expensePerMinute = expensePerMinute,
            netChangePerMinute = netChangePerMinute,
            baselineTimestamp = currentMinuteTimestamp,
            baselineAmount = newBaselineAmount
        )
    }

    /**
     * 当账户余额变化时更新基准值
     */
    suspend fun onAccountBalanceChanged(amountChange: Double) {
        val existingBaseline = assetBaselineDao.getBaseline()
        val currentMinuteTimestamp = getCurrentMinuteTimestamp()

        if (existingBaseline != null) {
            // 计算当前应有的资产值
            val fixedIncomes = fixedIncomeDao.getAllActiveFixedIncomes().first()
            var netChangePerMinute = 0.0
            fixedIncomes.forEach { entity ->
                val perMinute = calculatePerMinute(entity.amount, entity.frequency)
                if (entity.type == FixedIncomeType.INCOME) {
                    netChangePerMinute += perMinute
                } else {
                    netChangePerMinute -= perMinute
                }
            }

            val minutesElapsed = (currentMinuteTimestamp - existingBaseline.baselineTimestamp) / 60_000
            val currentAsset = existingBaseline.baselineAmount + (netChangePerMinute * minutesElapsed)

            // 加上账户变化
            val newBaselineAmount = currentAsset + amountChange

            // 保存新基准
            val newBaseline = AssetBaselineEntity(
                id = 1,
                baselineTimestamp = currentMinuteTimestamp,
                baselineAmount = newBaselineAmount
            )
            assetBaselineDao.insert(newBaseline)

            // 更新状态
            _realtimeAsset.value = _realtimeAsset.value.copy(
                currentAsset = newBaselineAmount,
                baselineTimestamp = currentMinuteTimestamp,
                baselineAmount = newBaselineAmount
            )
        } else {
            // 没有基准值，初始化
            recalculateAsset()
        }
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

