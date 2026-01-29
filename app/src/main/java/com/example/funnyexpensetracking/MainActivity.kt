package com.example.funnyexpensetracking

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.funnyexpensetracking.ui.transaction.TransactionFragment
import com.example.funnyexpensetracking.worker.AssetSnapshotWorker
import com.example.funnyexpensetracking.worker.StockPriceSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 加载TransactionFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TransactionFragment())
                .commit()
        }

        // 初始化后台任务
        setupWorkers()
    }

    /**
     * 设置后台定时任务
     */
    private fun setupWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        // 每15分钟保存一次资产快照
        val assetSnapshotRequest = PeriodicWorkRequestBuilder<AssetSnapshotWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            AssetSnapshotWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            assetSnapshotRequest
        )

        // 每15分钟同步一次股票价格（股票交易时间内）
        val stockSyncRequest = PeriodicWorkRequestBuilder<StockPriceSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            StockPriceSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            stockSyncRequest
        )
    }
}