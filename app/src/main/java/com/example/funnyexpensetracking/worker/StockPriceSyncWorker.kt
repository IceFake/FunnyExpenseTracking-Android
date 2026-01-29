package com.example.funnyexpensetracking.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 股票价格同步Worker - 定期刷新股票价格
 * TODO: 实现完整的股票价格同步功能
 */
class StockPriceSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // TODO: 同步股票价格
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "stock_price_sync_work"
    }
}

