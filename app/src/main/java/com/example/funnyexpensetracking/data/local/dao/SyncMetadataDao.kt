package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * 同步元数据DAO
 */
@Dao
interface SyncMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)

    @Update
    suspend fun update(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE tableName = :tableName")
    suspend fun getByTableName(tableName: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata")
    fun getAllMetadata(): Flow<List<SyncMetadataEntity>>

    @Query("UPDATE sync_metadata SET lastSyncTimestamp = :timestamp, lastSyncAttempt = :timestamp WHERE tableName = :tableName")
    suspend fun updateLastSyncTime(tableName: String, timestamp: Long)

    @Query("UPDATE sync_metadata SET lastError = :error, lastSyncAttempt = :timestamp WHERE tableName = :tableName")
    suspend fun updateSyncError(tableName: String, error: String?, timestamp: Long)

    @Query("UPDATE sync_metadata SET pendingCount = :count WHERE tableName = :tableName")
    suspend fun updatePendingCount(tableName: String, count: Int)
}

