package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.AssetBaselineEntity
import kotlinx.coroutines.flow.Flow

/**
 * 资产基准值DAO
 */
@Dao
interface AssetBaselineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(baseline: AssetBaselineEntity)

    @Query("SELECT * FROM asset_baseline WHERE id = 1")
    suspend fun getBaseline(): AssetBaselineEntity?

    @Query("SELECT * FROM asset_baseline WHERE id = 1")
    fun observeBaseline(): Flow<AssetBaselineEntity?>

    @Query("UPDATE asset_baseline SET baselineAmount = :amount, baselineTimestamp = :timestamp, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateBaseline(amount: Double, timestamp: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM asset_baseline")
    suspend fun deleteAll()
}

