package com.example.funnyexpensetracking.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.AssetSnapshotDao
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.local.dao.InvestmentDao
import com.example.funnyexpensetracking.data.local.entity.AssetSnapshotEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 资产快照 Worker — 每 15 分钟定时保存一次资产快照
 *
 * 计算逻辑与 [com.example.funnyexpensetracking.domain.usecase.RealtimeAssetCalculator] 保持一致：
 *   总资产 = 各账户余额之和 + 固定收支累计净额 + 投资/理财当前市值
 *
 * 快照数据拆分为：
 * - cashAsset  ：账户余额 + 固定收支净额（流动性较高的资产）
 * - stockAsset ：投资/理财当前市值
 * - totalAsset ：以上两项之和
 *
 * 同时会清理 30 天前的旧快照，避免数据库无限膨胀。
 */
@HiltWorker
class AssetSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountDao: AccountDao,
    private val fixedIncomeDao: FixedIncomeDao,
    private val investmentDao: InvestmentDao,
    private val assetSnapshotDao: AssetSnapshotDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始保存资产快照...")

            // 1. 获取各账户余额总和
            val totalAccountBalance = accountDao.getTotalBalance() ?: 0.0

            // 2. 获取固定收支累计净额
            val totalFixedIncome = fixedIncomeDao.getTotalAccumulatedIncome()
            val totalFixedExpense = fixedIncomeDao.getTotalAccumulatedExpense()
            val fixedIncomeNetAmount = totalFixedIncome - totalFixedExpense

            // 3. 获取投资/理财当前价值
            val totalInvestmentValue = investmentDao.getTotalCurrentValue()

            // 4. 计算资产分类
            val cashAsset = totalAccountBalance + fixedIncomeNetAmount
            val stockAsset = totalInvestmentValue
            val totalAsset = cashAsset + stockAsset

            // 5. 保存快照
            val snapshot = AssetSnapshotEntity(
                totalAsset = totalAsset,
                cashAsset = cashAsset,
                stockAsset = stockAsset,
                timestamp = System.currentTimeMillis()
            )
            assetSnapshotDao.insert(snapshot)

            // 6. 清理 30 天前的旧快照，防止数据库膨胀
            val thirtyDaysAgo = System.currentTimeMillis() - RETENTION_PERIOD_MS
            assetSnapshotDao.deleteOldSnapshots(thirtyDaysAgo)

            Log.d(TAG, "资产快照保存成功: 总资产=¥${"%.2f".format(totalAsset)}, " +
                    "现金=¥${"%.2f".format(cashAsset)}, 投资=¥${"%.2f".format(stockAsset)}")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "资产快照保存失败", e)
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "asset_snapshot_work"
        private const val TAG = "AssetSnapshotWorker"
        private const val MAX_RETRY_COUNT = 3
        private const val RETENTION_PERIOD_MS = 30L * 24 * 60 * 60 * 1000 // 30 天
    }
}

