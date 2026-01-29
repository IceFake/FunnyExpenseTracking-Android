package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.AssetSnapshot
import com.example.funnyexpensetracking.domain.model.AssetSummary
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.FixedIncomeType
import kotlinx.coroutines.flow.Flow

/**
 * 资产管理Repository接口
 */
interface AssetRepository {

    // ========== 固定收支管理 ==========

    /**
     * 获取所有生效的固定收支
     */
    fun getAllActiveFixedIncomes(): Flow<List<FixedIncome>>

    /**
     * 获取所有固定收支
     */
    fun getAllFixedIncomes(): Flow<List<FixedIncome>>

    /**
     * 根据类型获取固定收支
     */
    fun getFixedIncomesByType(type: FixedIncomeType): Flow<List<FixedIncome>>

    /**
     * 添加固定收支
     */
    suspend fun addFixedIncome(fixedIncome: FixedIncome): Long

    /**
     * 更新固定收支
     */
    suspend fun updateFixedIncome(fixedIncome: FixedIncome)

    /**
     * 删除固定收支
     */
    suspend fun deleteFixedIncome(fixedIncome: FixedIncome)

    /**
     * 更新固定收支生效状态
     */
    suspend fun updateFixedIncomeStatus(id: Long, isActive: Boolean)

    // ========== 资产快照管理 ==========

    /**
     * 保存资产快照
     */
    suspend fun saveAssetSnapshot(snapshot: AssetSnapshot): Long

    /**
     * 获取最新资产快照
     */
    suspend fun getLatestSnapshot(): AssetSnapshot?

    /**
     * 获取最新资产快照（Flow）
     */
    fun getLatestSnapshotFlow(): Flow<AssetSnapshot?>

    /**
     * 获取时间范围内的资产快照
     */
    fun getSnapshotsByTimeRange(startTime: Long, endTime: Long): Flow<List<AssetSnapshot>>

    /**
     * 获取最近的资产快照
     */
    fun getRecentSnapshots(limit: Int): Flow<List<AssetSnapshot>>

    // ========== 实时资产计算 ==========

    /**
     * 计算当前资产汇总（实时）
     */
    suspend fun calculateCurrentAssetSummary(): AssetSummary

    /**
     * 获取资产汇总Flow（用于实时更新UI）
     */
    fun getAssetSummaryFlow(): Flow<AssetSummary>
}

