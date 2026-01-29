package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.local.entity.TransactionType as EntityTransactionType
import com.example.funnyexpensetracking.data.remote.api.ExpenseApiService
import com.example.funnyexpensetracking.data.remote.dto.TransactionDto
import com.example.funnyexpensetracking.data.sync.SyncManager
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.domain.repository.TransactionRepository
import com.example.funnyexpensetracking.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易记录Repository实现类
 * 采用离线优先策略：
 * 1. 所有操作先写入本地数据库
 * 2. 网络可用时自动同步到云端
 * 3. 网络不可用时数据保存在本地，待网络恢复后同步
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val expenseApiService: ExpenseApiService,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        // 返回本地数据，过滤掉待删除的记录
        return transactionDao.getAllTransactionsExcludeStatus(SyncStatus.PENDING_DELETE)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            entities.filter { it.syncStatus != SyncStatus.PENDING_DELETE }
                .map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type.toEntityType()).map { entities ->
            entities.filter { it.syncStatus != SyncStatus.PENDING_DELETE }
                .map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category).map { entities ->
            entities.filter { it.syncStatus != SyncStatus.PENDING_DELETE }
                .map { it.toDomainModel() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getById(id)?.toDomainModel()
    }

    /**
     * 添加交易记录
     * 离线优先：先保存到本地，标记为待同步，然后尝试同步
     */
    override suspend fun addTransaction(transaction: Transaction): Long {
        val entity = transaction.toEntity().copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        val localId = transactionDao.insert(entity)

        // 尝试立即同步（如果网络可用）
        trySync()

        return localId
    }

    /**
     * 更新交易记录
     * 离线优先：先更新本地，标记为待同步
     */
    override suspend fun updateTransaction(transaction: Transaction) {
        val existingEntity = transactionDao.getById(transaction.id)
        val entity = transaction.toEntity().copy(
            serverId = existingEntity?.serverId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(entity)

        // 尝试立即同步
        trySync()
    }

    /**
     * 删除交易记录
     * 离线优先：先标记为待删除，网络可用时同步删除
     */
    override suspend fun deleteTransaction(transaction: Transaction) {
        val entity = transactionDao.getById(transaction.id)

        if (entity != null) {
            if (entity.serverId != null) {
                // 有服务器ID，标记为待删除
                transactionDao.markAsDeleted(transaction.id)
                trySync()
            } else {
                // 没有服务器ID（从未同步过），直接删除本地记录
                transactionDao.delete(entity)
            }
        }
    }

    override suspend fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double {
        return transactionDao.getTotalByTypeAndDateRange(
            type.toEntityType(),
            startDate,
            endDate,
            SyncStatus.PENDING_DELETE
        ) ?: 0.0
    }

    override suspend fun getCategoriesByType(type: TransactionType): List<String> {
        return transactionDao.getCategoriesByType(type.toEntityType())
    }

    /**
     * 与服务器同步数据
     */
    override suspend fun syncWithServer() {
        syncManager.syncTransactions()
    }

    /**
     * 从服务器拉取数据
     */
    suspend fun pullFromServer(startDate: Long, endDate: Long) {
        syncManager.pullFromServer(startDate, endDate)
    }

    /**
     * 获取待同步的记录数量
     */
    suspend fun getPendingSyncCount(): Int {
        return transactionDao.getPendingSyncCount()
    }

    /**
     * 检查网络并尝试同步
     */
    private suspend fun trySync() {
        if (networkMonitor.isNetworkAvailable()) {
            syncManager.syncTransactions()
        }
    }

    // ========== 转换函数 ==========

    private fun TransactionEntity.toDomainModel(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = type.toDomainType(),
            category = category,
            accountId = accountId,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type.toEntityType(),
            category = category,
            accountId = accountId,
            note = note,
            date = date,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
    }

    private fun TransactionType.toEntityType(): EntityTransactionType {
        return when (this) {
            TransactionType.INCOME -> EntityTransactionType.INCOME
            TransactionType.EXPENSE -> EntityTransactionType.EXPENSE
        }
    }

    private fun EntityTransactionType.toDomainType(): TransactionType {
        return when (this) {
            EntityTransactionType.INCOME -> TransactionType.INCOME
            EntityTransactionType.EXPENSE -> TransactionType.EXPENSE
        }
    }
}

