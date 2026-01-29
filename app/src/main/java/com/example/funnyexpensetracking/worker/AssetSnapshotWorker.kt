package com.example.funnyexpensetracking.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.funnyexpensetracking.domain.model.AssetSnapshot
import com.example.funnyexpensetracking.domain.repository.AssetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 资产快照Worker - 定期保存资产快照
 */
@HiltWorker
class AssetSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val assetRepository: AssetRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 计算当前资产
            val summary = assetRepository.calculateCurrentAssetSummary()

            // 保存快照
            val snapshot = AssetSnapshot(
                totalAsset = summary.totalAsset,
                cashAsset = summary.cashAsset,
                stockAsset = summary.stockAsset,
                timestamp = System.currentTimeMillis()
            )
            assetRepository.saveAssetSnapshot(snapshot)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "asset_snapshot_work"
    }
}

