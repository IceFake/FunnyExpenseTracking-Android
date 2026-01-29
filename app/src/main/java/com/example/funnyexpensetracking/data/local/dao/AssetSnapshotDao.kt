package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.AssetSnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * 资产快照DAO
 */
@Dao
interface AssetSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: AssetSnapshotEntity): Long

    @Query("SELECT * FROM asset_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): AssetSnapshotEntity?

    @Query("SELECT * FROM asset_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshotFlow(): Flow<AssetSnapshotEntity?>

    @Query("SELECT * FROM asset_snapshots WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getSnapshotsByTimeRange(startTime: Long, endTime: Long): Flow<List<AssetSnapshotEntity>>

    @Query("SELECT * FROM asset_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSnapshots(limit: Int): Flow<List<AssetSnapshotEntity>>

    @Query("DELETE FROM asset_snapshots WHERE timestamp < :beforeTime")
    suspend fun deleteOldSnapshots(beforeTime: Long)

    @Query("DELETE FROM asset_snapshots")
    suspend fun deleteAll()
}

