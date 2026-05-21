package com.example.funnyexpensetracking.data.sync

import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.SyncMetadataDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.AccountEntity
import com.example.funnyexpensetracking.data.local.entity.SyncMetadataEntity
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.local.entity.TransactionType
import com.example.funnyexpensetracking.data.remote.api.AccountApiService
import com.example.funnyexpensetracking.data.remote.api.ExpenseApiService
import com.example.funnyexpensetracking.data.remote.dto.*
import com.example.funnyexpensetracking.util.NetworkMonitor
import com.example.funnyexpensetracking.data.local.UserPreferencesManager
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
 *
 * 后端 Transaction ID 为 UUID 字符串，所有 id 对比均为 String 类型。
 */
@Singleton
class SyncManager @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val expenseApiService: ExpenseApiService,
    private val accountApiService: AccountApiService,
    private val networkMonitor: NetworkMonitor,
    private val userPreferencesManager: UserPreferencesManager
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
        observeNetworkAndSync()
        updatePendingSyncCount()
    }

    private fun observeNetworkAndSync() {
        scope.launch {
            networkMonitor.observeNetworkStatus()
                .filter { it == NetworkStatus.AVAILABLE }
                .collect {
                    if (_pendingSyncCount.value > 0 && userPreferencesManager.hasBackendSession()) {
                        syncAll()
                    }
                }
        }
    }

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
        if (!userPreferencesManager.hasBackendSession()) {
            return SyncResult.Error("未登录，数据已保存在本地，登录后将自动同步")
        }

        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用，数据已保存在本地")
        }

        _syncState.value = SyncState.Syncing
        var totalSynced = 0

        try {
            val transactionResult = syncTransactions()
            if (transactionResult is SyncResult.Success) {
                totalSynced += transactionResult.data
            }

            val accountResult = syncAccounts()
            if (accountResult is SyncResult.Success) {
                totalSynced += accountResult.data
            }

            updatePendingSyncCount()
            _syncState.value = SyncState.Success(totalSynced)
            return SyncResult.Success(totalSynced)
        } catch (e: Exception) {
            val errorMessage = "同步失败: ${e.message}"
            _syncState.value = SyncState.Error(errorMessage, e)
            return SyncResult.Error(errorMessage, e)
        }
    }

    // ========================================================================
    //  交易同步
    // ========================================================================

    suspend fun syncTransactions(): SyncResult<Int> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        try {
            val pendingTransactions = transactionDao.getPendingSyncTransactions()
            if (pendingTransactions.isEmpty()) {
                return SyncResult.Success(0)
            }

            val toUpload = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            val toDelete = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

            var syncedCount = 0

            // --- 上传新增/修改 ---
            if (toUpload.isNotEmpty()) {
                val dtos = toUpload.map { it.toSyncDto() }
                val lastSyncTime = syncMetadataDao.getByTableName(TABLE_TRANSACTIONS)?.lastSyncTimestamp ?: 0
                val request = SyncRequest(dtos, lastSyncTime)

                val response = expenseApiService.syncTransactions(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 200) {
                        // 后端返回 SyncResponse，取 syncedTransactions 列表
                        val serverTransactions = body.data?.syncedTransactions.orEmpty()

                        // 处理服务端返回的 serverId 映射
                        for (entity in toUpload) {
                            val match = findServerTransactionForLocal(entity, serverTransactions)
                            val serverId = match?.id?.takeIf { it.isNotBlank() }
                            if (serverId != null) {
                                transactionDao.updateServerId(entity.id, serverId, SyncStatus.SYNCED)
                            } else {
                                transactionDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                            }
                        }
                        syncedCount += toUpload.size
                    } else {
                        // 处理业务错误
                        return SyncResult.Error("同步交易失败: ${body?.message}")
                    }
                } else if (response.code() == 409) {
                    // --- 冲突处理 ---
                    return handleTransactionConflict(toUpload)
                } else {
                    return SyncResult.Error("服务器返回错误: ${response.code()}")
                }
            }

            // --- 处理待删除 ---
            if (toDelete.isNotEmpty()) {
                for (entity in toDelete) {
                    entity.serverId?.let { serverId ->
                        val response = expenseApiService.deleteTransaction(serverId)
                        if (response.isSuccessful) {
                            transactionDao.delete(entity)
                            syncedCount++
                        }
                    } ?: run {
                        // 本地记录无 serverId，直接删除
                        transactionDao.delete(entity)
                        syncedCount++
                    }
                }
            }

            updateSyncMetadata(TABLE_TRANSACTIONS)
            return SyncResult.Success(syncedCount)
        } catch (e: Exception) {
            updateSyncError(TABLE_TRANSACTIONS, e.message)
            return SyncResult.Error("同步交易记录失败: ${e.message}", e)
        }
    }

    /**
     * 处理 409 冲突：LWW（最后写入者胜出）
     */
    private suspend fun handleTransactionConflict(toUpload: List<TransactionEntity>): SyncResult<Int> {
        try {
            val now = System.currentTimeMillis()
            val response = expenseApiService.getTransactions(0, now)
            if (response.isSuccessful && response.body()?.code == 200) {
                val serverTransactions = response.body()?.data ?: emptyList()
                for (serverTx in serverTransactions) {
                    serverTx.id?.let { serverId ->
                        val localEntity = transactionDao.getByServerId(serverId)
                        if (localEntity != null) {
                            if (serverTx.updatedAt != null &&
                                (localEntity.updatedAt < serverTx.updatedAt ||
                                        localEntity.syncStatus == SyncStatus.CONFLICT)) {
                                transactionDao.update(serverTx.toEntity().copy(id = localEntity.id))
                            }
                        }
                    }
                }
                for (entity in toUpload) {
                    transactionDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                }
                updateSyncMetadata(TABLE_TRANSACTIONS)
                return SyncResult.Success(toUpload.size)
            }
            return SyncResult.Error("冲突处理失败")
        } catch (e: Exception) {
            return SyncResult.Error("冲突处理失败: ${e.message}")
        }
    }

    // ========================================================================
    //  账户同步
    // ========================================================================

    suspend fun syncAccounts(): SyncResult<Int> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        try {
            val pendingAccounts = accountDao.getPendingSyncAccounts()
            if (pendingAccounts.isEmpty()) {
                return SyncResult.Success(0)
            }

            val toUpload = pendingAccounts.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            val toDelete = pendingAccounts.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

            var syncedCount = 0

            if (toUpload.isNotEmpty()) {
                val dtos = toUpload.map { it.toSyncDto() }
                val lastSyncTime = syncMetadataDao.getByTableName(TABLE_ACCOUNTS)?.lastSyncTimestamp ?: 0
                val request = AccountSyncRequest(dtos, lastSyncTime)

                val response = accountApiService.syncAccounts(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 200) {
                        val serverAccounts = body.data ?: emptyList()

                        for (entity in toUpload) {
                            val match = findServerAccountForLocal(entity, serverAccounts)
                            val serverId = match?.id?.takeIf { it.isNotBlank() }
                            if (serverId != null) {
                                accountDao.updateServerId(entity.id, serverId, SyncStatus.SYNCED)
                            } else {
                                accountDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                            }
                        }
                        syncedCount += toUpload.size
                    } else {
                        return SyncResult.Error("同步账户失败: ${body?.message}")
                    }
                } else if (response.code() == 409) {
                    return handleAccountConflict(toUpload)
                } else {
                    return SyncResult.Error("服务器返回错误: ${response.code()}")
                }
            }

            if (toDelete.isNotEmpty()) {
                for (entity in toDelete) {
                    entity.serverId?.let { serverId ->
                        val response = accountApiService.deleteAccount(serverId)
                        if (response.isSuccessful) {
                            accountDao.delete(entity)
                            syncedCount++
                        }
                    } ?: run {
                        accountDao.delete(entity)
                        syncedCount++
                    }
                }
            }

            updateSyncMetadata(TABLE_ACCOUNTS)
            return SyncResult.Success(syncedCount)
        } catch (e: Exception) {
            updateSyncError(TABLE_ACCOUNTS, e.message)
            return SyncResult.Error("同步账户数据失败: ${e.message}", e)
        }
    }

    private suspend fun handleAccountConflict(toUpload: List<AccountEntity>): SyncResult<Int> {
        try {
            val response = accountApiService.getAccounts()
            if (response.isSuccessful && response.body()?.code == 200) {
                val serverAccounts = response.body()?.data ?: emptyList()
                for (serverAcct in serverAccounts) {
                        serverAcct.id?.let { serverId ->
                            val localEntity = accountDao.getByServerId(serverId)
                        if (localEntity != null) {
                            if (serverAcct.updatedAt != null &&
                                localEntity.updatedAt < serverAcct.updatedAt) {
                                accountDao.update(serverAcct.toEntity().copy(id = localEntity.id))
                            }
                        }
                    }
                }
                for (entity in toUpload) {
                    accountDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                }
                updateSyncMetadata(TABLE_ACCOUNTS)
                return SyncResult.Success(toUpload.size)
            }
            return SyncResult.Error("账户冲突处理失败")
        } catch (e: Exception) {
            return SyncResult.Error("账户冲突处理失败: ${e.message}")
        }
    }

    // ========================================================================
    //  从服务器拉取
    // ========================================================================

    suspend fun pullTransactionsFromServer(startDate: Long, endDate: Long): SyncResult<List<TransactionEntity>> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        try {
            val response = expenseApiService.getTransactions(startDate, endDate)
            if (response.isSuccessful && response.body()?.code == 200) {
                val serverTransactions = response.body()?.data ?: emptyList()
                val entities = serverTransactions.map { it.toEntity() }

                for (entity in entities) {
                    val sid = entity.serverId
                    if (sid != null) {
                        val localEntity = transactionDao.getByServerId(sid)
                        if (localEntity != null) {
                            if (localEntity.syncStatus != SyncStatus.PENDING_UPLOAD) {
                                transactionDao.update(entity.copy(id = localEntity.id))
                            }
                        } else {
                            transactionDao.insert(entity)
                        }
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

    // ========================================================================
    //  同步元数据
    // ========================================================================

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

    fun needsSync(): Boolean {
        return _pendingSyncCount.value > 0
    }

    fun refreshPendingCount() {
        updatePendingSyncCount()
    }

    // ========================================================================
    //  实体 <-> DTO 转换
    // ========================================================================

    private fun findServerTransactionForLocal(
        local: TransactionEntity,
        serverList: List<TransactionDto>
    ): TransactionDto? {
        val localServerId = local.serverId
        if (!localServerId.isNullOrBlank()) {
            serverList.firstOrNull { it.id == localServerId }?.let { return it }
        }
        // 按时间戳最接近匹配
        return serverList.minByOrNull { dto ->
            kotlin.math.abs((dto.createdAt ?: dto.date) - local.createdAt)
        }
    }

    private fun findServerAccountForLocal(
        local: AccountEntity,
        serverList: List<AccountDto>
    ): AccountDto? {
        val localServerId = local.serverId
        if (!localServerId.isNullOrBlank()) {
            serverList.firstOrNull { it.id == localServerId }?.let { return it }
        }
        return serverList.minByOrNull { dto ->
            kotlin.math.abs((dto.createdAt ?: 0L) - local.createdAt)
        }
    }

    // ---------- Transaction ----------

    private fun TransactionEntity.toSyncDto(): TransactionDto {
        return TransactionDto(
            id = serverId,
            amount = amount,
            type = type.name,
            category = category,
            note = note,
            date = date,
            accountId = accountId.takeIf { it > 0 },
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = null
        )
    }

    private fun TransactionDto.toEntity(): TransactionEntity {
        return TransactionEntity(
            serverId = id,
            amount = amount,
            type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.EXPENSE },
            category = category,
            accountId = accountId ?: 0,
            note = note,
            date = date,
            createdAt = createdAt ?: date,
            updatedAt = updatedAt ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED,
            lastSyncAt = System.currentTimeMillis()
        )
    }

    // ---------- Account ----------

    private fun AccountEntity.toSyncDto(): AccountDto {
        return AccountDto(
            id = serverId,
            name = name,
            icon = icon,
            balance = balance,
            isDefault = isDefault,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun AccountDto.toEntity(): AccountEntity {
        return AccountEntity(
            serverId = id,
            name = name,
            icon = icon,
            balance = balance,
            isDefault = isDefault,
            sortOrder = sortOrder,
            createdAt = createdAt ?: System.currentTimeMillis(),
            updatedAt = updatedAt ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED,
            lastSyncAt = System.currentTimeMillis()
        )
    }
}
