package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.AccountEntity
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * 账户DAO
 */
@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>): List<Long>

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE serverId = :serverId")
    suspend fun getByServerId(serverId: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE syncStatus != :excludeStatus ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllAccountsExcludeStatus(excludeStatus: SyncStatus = SyncStatus.PENDING_DELETE): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): AccountEntity?

    @Query("UPDATE accounts SET balance = balance + :amount, updatedAt = :updateTime, syncStatus = :status WHERE id = :accountId")
    suspend fun updateBalance(
        accountId: Long,
        amount: Double,
        updateTime: Long = System.currentTimeMillis(),
        status: SyncStatus = SyncStatus.PENDING_UPLOAD
    )

    @Query("UPDATE accounts SET isDefault = 0, updatedAt = :updateTime, syncStatus = :status")
    suspend fun clearDefaultAccount(
        updateTime: Long = System.currentTimeMillis(),
        status: SyncStatus = SyncStatus.PENDING_UPLOAD
    )

    @Query("UPDATE accounts SET isDefault = 1, updatedAt = :updateTime, syncStatus = :status WHERE id = :accountId")
    suspend fun setDefaultAccount(
        accountId: Long,
        updateTime: Long = System.currentTimeMillis(),
        status: SyncStatus = SyncStatus.PENDING_UPLOAD
    )

    @Query("SELECT SUM(balance) FROM accounts WHERE syncStatus != :excludeStatus")
    suspend fun getTotalBalance(excludeStatus: SyncStatus = SyncStatus.PENDING_DELETE): Double?

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    // ========== 同步相关查询 ==========

    /**
     * 获取待同步的账户记录
     */
    @Query("SELECT * FROM accounts WHERE syncStatus IN (:statuses)")
    suspend fun getPendingSyncAccounts(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_DELETE)
    ): List<AccountEntity>

    /**
     * 获取指定同步状态的账户
     */
    @Query("SELECT * FROM accounts WHERE syncStatus = :status")
    suspend fun getAccountsByStatus(status: SyncStatus): List<AccountEntity>

    /**
     * 更新同步状态
     */
    @Query("UPDATE accounts SET syncStatus = :status, lastSyncAt = :syncTime WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, syncTime: Long = System.currentTimeMillis())

    /**
     * 批量更新同步状态
     */
    @Query("UPDATE accounts SET syncStatus = :status, lastSyncAt = :syncTime WHERE id IN (:ids)")
    suspend fun updateSyncStatusBatch(ids: List<Long>, status: SyncStatus, syncTime: Long = System.currentTimeMillis())

    /**
     * 更新服务器ID
     */
    @Query("UPDATE accounts SET serverId = :serverId, syncStatus = :status WHERE id = :localId")
    suspend fun updateServerId(localId: Long, serverId: Long, status: SyncStatus = SyncStatus.SYNCED)

    /**
     * 标记为待删除（软删除）
     */
    @Query("UPDATE accounts SET syncStatus = :status WHERE id = :id")
    suspend fun markAsDeleted(id: Long, status: SyncStatus = SyncStatus.PENDING_DELETE)

    /**
     * 真正删除已同步删除的记录
     */
    @Query("DELETE FROM accounts WHERE syncStatus = :status AND lastSyncAt IS NOT NULL")
    suspend fun deleteSyncedDeletions(status: SyncStatus = SyncStatus.PENDING_DELETE)

    /**
     * 获取待同步记录数量
     */
    @Query("SELECT COUNT(*) FROM accounts WHERE syncStatus IN (:statuses)")
    suspend fun getPendingSyncCount(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_DELETE)
    ): Int

    /**
     * 获取上次同步后的变更记录
     */
    @Query("SELECT * FROM accounts WHERE updatedAt > :lastSyncTime")
    suspend fun getChangedSince(lastSyncTime: Long): List<AccountEntity>
}




