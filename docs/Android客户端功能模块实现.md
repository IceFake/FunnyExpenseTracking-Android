# 数据管理模块实现

## SyncManager 核心代码片段（节选）

> 说明：以下仅保留同步管理流程中的关键方法，用于说明离线优先场景下的数据同步机制。

```kotlin
private fun observeNetworkAndSync() {
    scope.launch {
        networkMonitor.observeNetworkStatus()
            .filter { it == NetworkStatus.AVAILABLE }
            .collect {
                if (_pendingSyncCount.value > 0) {
                    syncAll()
                }
            }
    }
}
```

```kotlin
suspend fun syncAll(): SyncResult<Int> {
    if (!networkMonitor.isNetworkAvailable()) {
        return SyncResult.Error("网络不可用，数据已保存在本地")
    }

    _syncState.value = SyncState.Syncing
    var totalSynced = 0

    return try {
        val transactionResult = syncTransactions()
        if (transactionResult is SyncResult.Success) {
            totalSynced += transactionResult.data
        }

        val accountResult = syncAccounts()
        if (accountResult is SyncResult.Success) {
            totalSynced += accountResult.data
        }

        _syncState.value = SyncState.Success(totalSynced)
        updatePendingSyncCount()
        SyncResult.Success(totalSynced)
    } catch (e: Exception) {
        val errorMessage = "同步失败: ${e.message}"
        _syncState.value = SyncState.Error(errorMessage, e)
        SyncResult.Error(errorMessage, e)
    }
}
```

```kotlin
suspend fun syncTransactions(): SyncResult<Int> {
    if (!networkMonitor.isNetworkAvailable()) return SyncResult.Error("网络不可用")

    return try {
        val pendingTransactions = transactionDao.getPendingSyncTransactions()
        if (pendingTransactions.isEmpty()) return SyncResult.Success(0)

        val toUpload = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
        val toDelete = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_DELETE }
        var syncedCount = 0

        if (toUpload.isNotEmpty()) {
            val dtos = toUpload.map { it.toDto() }
            val lastSyncTime = syncMetadataDao.getByTableName(TABLE_TRANSACTIONS)?.lastSyncTimestamp ?: 0
            val request = SyncRequest(dtos, lastSyncTime)
            val response = expenseApiService.syncTransactions(request)

            if (response.isSuccessful && response.body()?.code == 200) {
                val serverTransactions = response.body()?.data ?: emptyList()
                toUpload.forEachIndexed { index, entity ->
                    val serverData = serverTransactions.getOrNull(index)
                    if (serverData != null) {
                        transactionDao.updateServerId(entity.id, serverData.id)
                    } else {
                        transactionDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                    }
                }
                syncedCount += toUpload.size
            }
        }

        if (toDelete.isNotEmpty()) {
            for (entity in toDelete) {
                entity.serverId?.let { serverId ->
                    val response = expenseApiService.deleteTransaction(serverId)
                    if (response.isSuccessful) {
                        transactionDao.delete(entity)
                        syncedCount++
                    }
                } ?: run {
                    transactionDao.delete(entity)
                    syncedCount++
                }
            }
        }

        updateSyncMetadata(TABLE_TRANSACTIONS)
        SyncResult.Success(syncedCount)
    } catch (e: Exception) {
        updateSyncError(TABLE_TRANSACTIONS, e.message)
        SyncResult.Error("同步交易记录失败: ${e.message}", e)
    }
}
```

```kotlin
suspend fun syncAccounts(): SyncResult<Int> {
    if (!networkMonitor.isNetworkAvailable()) return SyncResult.Error("网络不可用")

    return try {
        val pendingAccounts = accountDao.getPendingSyncAccounts()
        if (pendingAccounts.isEmpty()) return SyncResult.Success(0)

        val ids = pendingAccounts.map { it.id }
        accountDao.updateSyncStatusBatch(ids, SyncStatus.SYNCED)
        updateSyncMetadata(TABLE_ACCOUNTS)
        SyncResult.Success(pendingAccounts.size)
    } catch (e: Exception) {
        updateSyncError(TABLE_ACCOUNTS, e.message)
        SyncResult.Error("同步账户数据失败: ${e.message}", e)
    }
}
```

```kotlin
private suspend fun updateSyncMetadata(tableName: String) {
    val now = System.currentTimeMillis()
    val existingMetadata = syncMetadataDao.getByTableName(tableName)

    if (existingMetadata != null) {
        syncMetadataDao.updateLastSyncTime(tableName, now)
    } else {
        syncMetadataDao.insert(
            SyncMetadataEntity(
                tableName = tableName,
                lastSyncTimestamp = now,
                lastSyncAttempt = now
            )
        )
    }
}
```

```kotlin
private suspend fun updateSyncError(tableName: String, error: String?) {
    val now = System.currentTimeMillis()
    val existingMetadata = syncMetadataDao.getByTableName(tableName)

    if (existingMetadata != null) {
        syncMetadataDao.updateSyncError(tableName, error, now)
    } else {
        syncMetadataDao.insert(
            SyncMetadataEntity(
                tableName = tableName,
                lastSyncAttempt = now,
                lastError = error
            )
        )
    }
}
```

## 核心方法分析说明

### 1. `observeNetworkAndSync`
该方法构建了同步触发的入口条件，将网络可用状态与待同步计数联合判定。当网络从不可用转为可用且本地存在积压数据时，系统自动调用 `syncAll`。这一设计避免了无效轮询，也保证了离线期间产生的数据能够在连接恢复后被及时处理。

### 2. `syncAll`
该方法负责同步流程的统一编排，先进行网络可达性校验，再按交易数据与账户数据两个子流程顺序执行。方法通过 `SyncState` 显式发布同步中、成功与失败状态，使 UI 层能够基于状态流完成一致性反馈。其结果汇总策略将多子任务的同步数量合并为单一指标，便于后续监控与统计。

### 3. `syncTransactions`
该方法是交易数据同步的核心执行单元，采用“待上传”与“待删除”分流机制处理本地变更。上传阶段通过 `lastSyncTimestamp` 构造增量请求，减少重复传输，删除阶段则依据 `serverId` 决定远端删除或本地直接清理。方法末尾统一更新同步元数据，并在异常路径记录错误信息，从而维持同步状态的可追踪性。

### 4. `syncAccounts`
该方法承担账户数据的同步闭环，目前以本地状态收敛为主，即批量将待同步账户标记为已同步，并写入同步时间元数据。尽管远端账户同步接口尚处于占位实现，该方法仍保持与交易同步一致的错误记录策略，保证了管理逻辑在结构层面的统一性。

### 5. `updateSyncMetadata`
该方法负责维护同步成功后的元信息，核心作用是更新或初始化指定数据表的最近同步时间与尝试时间。该机制为增量同步提供时间基线，也为系统评估同步频率与健康状态提供基础数据。

### 6. `updateSyncError`
该方法用于落库失败上下文，在同步异常时记录错误文本与失败时间。通过将错误信息与数据表维度绑定，系统可以实现更细粒度的故障定位，并为后续重试策略和问题排查提供可靠依据。
