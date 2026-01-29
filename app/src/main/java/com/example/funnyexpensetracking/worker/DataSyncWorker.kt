package com.example.funnyexpensetracking.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.funnyexpensetracking.data.sync.SyncManager
import com.example.funnyexpensetracking.data.sync.SyncResult
import com.example.funnyexpensetracking.util.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 数据同步 Worker
 * 在后台定期执行数据同步任务
 */
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 检查网络状态
        if (!networkMonitor.isNetworkAvailable()) {
            return Result.retry()
        }

        return when (val result = syncManager.syncAll()) {
            is SyncResult.Success -> {
                Result.success(
                    workDataOf(KEY_SYNCED_COUNT to result.data)
                )
            }
            is SyncResult.Error -> {
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(KEY_ERROR_MESSAGE to result.message)
                    )
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "data_sync_worker"
        const val KEY_SYNCED_COUNT = "synced_count"
        const val KEY_ERROR_MESSAGE = "error_message"
        private const val MAX_RETRY_COUNT = 3

        /**
         * 创建一次性同步请求
         */
        fun createOneTimeRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
        }

        /**
         * 创建定期同步请求（每15分钟）
         */
        fun createPeriodicRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            return PeriodicWorkRequestBuilder<DataSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
        }
    }
}

