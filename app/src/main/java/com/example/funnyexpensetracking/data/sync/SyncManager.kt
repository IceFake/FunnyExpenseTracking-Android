package com.example.funnyexpensetracking.data.sync

import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.SyncMetadataDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.AccountEntity
import com.example.funnyexpensetracking.data.local.entity.SyncMetadataEntity
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.remote.api.ExpenseApiService
import com.example.funnyexpensetracking.data.remote.dto.SyncRequest
import com.example.funnyexpensetracking.data.remote.dto.TransactionDto
import com.example.funnyexpensetracking.util.NetworkMonitor
import com.example.funnyexpensetracking.util.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步状态
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val syncedCount: Int) : SyncState()
    data class Error(val message: String, val exception: Throwable? = null) : SyncState()
}

/**
 * 同步结果
 */
sealed class SyncResult<out T> {
    data class Success<T>(val data: T) : SyncResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : SyncResult<Nothing>()
}

/**
 * 数据同步管理器
 * 负责管理本地数据与云端的同步
 */
@Singleton
class SyncManager @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val expenseApiService: ExpenseApiService,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    companion object {
        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_ACCOUNTS = "accounts"
    }

    init {
        // 监听网络状态，网络恢复时自动同步
        observeNetworkAndSync()
        // 初始化待同步计数
        updatePendingSyncCount()
    }

    /**
     * 监听网络状态变化并自动同步
     */
    private fun observeNetworkAndSync() {
        scope.launch {
            networkMonitor.observeNetworkStatus()
                .filter { it == NetworkStatus.AVAILABLE }
                .collect {
                    // 网络恢复时，自动同步待处理的数据
                    if (_pendingSyncCount.value > 0) {
                        syncAll()
                    }
                }
        }
    }

    /**
     * 更新待同步计数
     */
    private fun updatePendingSyncCount() {
        scope.launch {
            val transactionCount = transactionDao.getPendingSyncCount()
            val accountCount = accountDao.getPendingSyncCount()
            _pendingSyncCount.value = transactionCount + accountCount
        }
    }

    /**
     * 同步所有数据
     */
    suspend fun syncAll(): SyncResult<Int> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用，数据已保存在本地")
        }

        _syncState.value = SyncState.Syncing
        var totalSynced = 0

        try {
            // 同步交易记录
            val transactionResult = syncTransactions()
            if (transactionResult is SyncResult.Success) {
                totalSynced += transactionResult.data
            }

            // 同步账户数据
            val accountResult = syncAccounts()
            if (accountResult is SyncResult.Success) {
                totalSynced += accountResult.data
            }

            _syncState.value = SyncState.Success(totalSynced)
            updatePendingSyncCount()
            return SyncResult.Success(totalSynced)

        } catch (e: Exception) {
            val errorMessage = "同步失败: ${e.message}"
            _syncState.value = SyncState.Error(errorMessage, e)
            return SyncResult.Error(errorMessage, e)
        }
    }

    /**
     * 同步交易记录
     */
    suspend fun syncTransactions(): SyncResult<Int> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        try {
            // 获取待同步的交易记录
            val pendingTransactions = transactionDao.getPendingSyncTransactions()
            if (pendingTransactions.isEmpty()) {
                return SyncResult.Success(0)
            }

            // 分离待上传和待删除的记录
            val toUpload = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            val toDelete = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

            var syncedCount = 0

            // 上传新增/修改的记录
            if (toUpload.isNotEmpty()) {
                val dtos = toUpload.map { it.toDto() }
                val lastSyncTime = syncMetadataDao.getByTableName(TABLE_TRANSACTIONS)?.lastSyncTimestamp ?: 0
                val request = SyncRequest(dtos, lastSyncTime)

                val response = expenseApiService.syncTransactions(request)
                if (response.isSuccessful && response.body()?.code == 200) {
                    val serverTransactions = response.body()?.data ?: emptyList()
                    
                    // 更新本地记录的服务器ID和同步状态
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

            // 处理待删除的记录
            if (toDelete.isNotEmpty()) {
                for (entity in toDelete) {
                    entity.serverId?.let { serverId ->
                        val response = expenseApiService.deleteTransaction(serverId)
                        if (response.isSuccessful) {
                            transactionDao.delete(entity)
                            syncedCount++
                        }
                    } ?: run {
                        // 没有服务器ID的直接删除
                        transactionDao.delete(entity)
                        syncedCount++
                    }
                }
            }

            // 更新同步元数据
            updateSyncMetadata(TABLE_TRANSACTIONS)

            return SyncResult.Success(syncedCount)

        } catch (e: Exception) {
            updateSyncError(TABLE_TRANSACTIONS, e.message)
            return SyncResult.Error("同步交易记录失败: ${e.message}", e)
        }
    }

    /**
     * 同步账户数据
     */
    suspend fun syncAccounts(): SyncResult<Int> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        try {
            val pendingAccounts = accountDao.getPendingSyncAccounts()
            if (pendingAccounts.isEmpty()) {
                return SyncResult.Success(0)
            }

            // TODO: 实现账户同步API调用
            // 暂时只更新本地同步状态
            val ids = pendingAccounts.map { it.id }
            accountDao.updateSyncStatusBatch(ids, SyncStatus.SYNCED)

            updateSyncMetadata(TABLE_ACCOUNTS)
            return SyncResult.Success(pendingAccounts.size)

        } catch (e: Exception) {
            updateSyncError(TABLE_ACCOUNTS, e.message)
            return SyncResult.Error("同步账户数据失败: ${e.message}", e)
        }
    }

    /**
     * 从服务器拉取数据
     */
    suspend fun pullFromServer(startDate: Long, endDate: Long): SyncResult<List<TransactionEntity>> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        try {
            val response = expenseApiService.getTransactions(startDate, endDate)
            if (response.isSuccessful && response.body()?.code == 200) {
                val serverTransactions = response.body()?.data ?: emptyList()
                val entities = serverTransactions.map { it.toEntity() }
                
                // 合并服务器数据到本地（使用服务器数据覆盖本地数据）
                for (entity in entities) {
                    val localEntity = transactionDao.getByServerId(entity.serverId ?: 0)
                    if (localEntity != null) {
                        // 更新现有记录
                        transactionDao.update(entity.copy(id = localEntity.id))
                    } else {
                        // 插入新记录
                        transactionDao.insert(entity)
                    }
                }

                return SyncResult.Success(entities)
            } else {
                return SyncResult.Error("服务器返回错误: ${response.body()?.message}")
            }

        } catch (e: Exception) {
            return SyncResult.Error("拉取数据失败: ${e.message}", e)
        }
    }

    /**
     * 更新同步元数据
     */
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

    /**
     * 更新同步错误信息
     */
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

    /**
     * 检查是否需要同步
     */
    fun needsSync(): Boolean {
        return _pendingSyncCount.value > 0
    }

    /**
     * 手动刷新待同步计数
     */
    fun refreshPendingCount() {
        updatePendingSyncCount()
    }

    // ========== 扩展函数：实体与DTO转换 ==========

    private fun TransactionEntity.toDto(): TransactionDto {
        return TransactionDto(
            id = serverId ?: 0,
            amount = amount,
            type = type.name,
            category = category,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    private fun TransactionDto.toEntity(): TransactionEntity {
        return TransactionEntity(
            serverId = id,
            amount = amount,
            type = com.example.funnyexpensetracking.data.local.entity.TransactionType.valueOf(type),
            category = category,
            accountId = 0, // TODO: 需要从DTO中获取账户信息
            note = note,
            date = date,
            createdAt = createdAt,
            syncStatus = SyncStatus.SYNCED,
            lastSyncAt = System.currentTimeMillis()
        )
    }
}

