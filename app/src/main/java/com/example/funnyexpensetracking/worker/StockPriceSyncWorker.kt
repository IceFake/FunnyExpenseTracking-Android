package com.example.funnyexpensetracking.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.funnyexpensetracking.domain.repository.StockRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 股票价格同步Worker - 定期刷新股票价格
 */
@HiltWorker
class StockPriceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val stockRepository: StockRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            stockRepository.refreshAllStockPrices()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "stock_price_sync_work"
    }
}

