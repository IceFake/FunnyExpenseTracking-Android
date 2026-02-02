package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * 交易记录DAO
 */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE serverId = :serverId")
    suspend fun getByServerId(serverId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE syncStatus != :status ORDER BY date DESC")
    fun getAllTransactionsExcludeStatus(status: SyncStatus = SyncStatus.PENDING_DELETE): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate AND syncStatus != :excludeStatus")
    suspend fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long,
        excludeStatus: SyncStatus = SyncStatus.PENDING_DELETE
    ): Double?

    @Query("SELECT DISTINCT category FROM transactions WHERE type = :type")
    suspend fun getCategoriesByType(type: TransactionType): List<String>

    /**
     * 获取所有普通收入的总额
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'INCOME' AND syncStatus != 'PENDING_DELETE'")
    suspend fun getTotalIncome(): Double

    /**
     * 获取所有普通支出的总额
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND syncStatus != 'PENDING_DELETE'")
    suspend fun getTotalExpense(): Double

    /**
     * 获取普通收支净额（收入-支出）
     */
    suspend fun getNetTransactionAmount(): Double {
        return getTotalIncome() - getTotalExpense()
    }

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // ========== 同步相关查询 ==========

    /**
     * 获取待同步的交易记录（包括待上传和待删除）
     */
    @Query("SELECT * FROM transactions WHERE syncStatus IN (:statuses)")
    suspend fun getPendingSyncTransactions(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_DELETE)
    ): List<TransactionEntity>

    /**
     * 获取待上传的交易记录
     */
    @Query("SELECT * FROM transactions WHERE syncStatus = :status")
    suspend fun getTransactionsByStatus(status: SyncStatus): List<TransactionEntity>

    /**
     * 更新同步状态
     */
    @Query("UPDATE transactions SET syncStatus = :status, lastSyncAt = :syncTime WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, syncTime: Long = System.currentTimeMillis())

    /**
     * 批量更新同步状态
     */
    @Query("UPDATE transactions SET syncStatus = :status, lastSyncAt = :syncTime WHERE id IN (:ids)")
    suspend fun updateSyncStatusBatch(ids: List<Long>, status: SyncStatus, syncTime: Long = System.currentTimeMillis())

    /**
     * 更新服务器ID
     */
    @Query("UPDATE transactions SET serverId = :serverId, syncStatus = :status WHERE id = :localId")
    suspend fun updateServerId(localId: Long, serverId: Long, status: SyncStatus = SyncStatus.SYNCED)

    /**
     * 标记为待删除（软删除）
     */
    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
    suspend fun markAsDeleted(id: Long, status: SyncStatus = SyncStatus.PENDING_DELETE)

    /**
     * 真正删除已同步删除的记录
     */
    @Query("DELETE FROM transactions WHERE syncStatus = :status AND lastSyncAt IS NOT NULL")
    suspend fun deleteSyncedDeletions(status: SyncStatus = SyncStatus.PENDING_DELETE)

    /**
     * 获取待同步记录数量
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE syncStatus IN (:statuses)")
    suspend fun getPendingSyncCount(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_DELETE)
    ): Int

    /**
     * 获取上次同步后的变更记录
     */
    @Query("SELECT * FROM transactions WHERE updatedAt > :lastSyncTime")
    suspend fun getChangedSince(lastSyncTime: Long): List<TransactionEntity>
}




