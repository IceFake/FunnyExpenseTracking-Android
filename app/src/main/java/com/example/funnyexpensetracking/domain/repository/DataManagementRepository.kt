package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.BackupData

/**
 * 数据管理Repository接口
 * 用于导入/导出/清除所有用户数据
 */
interface DataManagementRepository {

    /**
     * 导出所有数据
     * @return 包含所有用户数据的备份对象
     */
    suspend fun exportAllData(): BackupData

    /**
     * 导入数据（增量导入，不覆盖现有数据）
     * 相同项优先叠加数量，无数量则叠加金额
     * @param backupData 要导入的备份数据
     * @return 导入结果统计
     */
    suspend fun importData(backupData: BackupData): ImportResult

    /**
     * 清除所有数据
     */
    suspend fun clearAllData()
}

/**
 * 导入结果统计
 */
data class ImportResult(
    val transactionsAdded: Int = 0,
    val accountsAdded: Int = 0,
    val accountsMerged: Int = 0,
    val fixedIncomesAdded: Int = 0,
    val fixedIncomesMerged: Int = 0,
    val investmentsAdded: Int = 0,
    val investmentsMerged: Int = 0,
    val stockHoldingsAdded: Int = 0,
    val stockHoldingsMerged: Int = 0,
    val assetSnapshotsAdded: Int = 0,
    val errors: List<String> = emptyList()
) {
    fun getTotalAdded(): Int = transactionsAdded + accountsAdded + fixedIncomesAdded +
        investmentsAdded + stockHoldingsAdded + assetSnapshotsAdded

    fun getTotalMerged(): Int = accountsMerged + fixedIncomesMerged +
        investmentsMerged + stockHoldingsMerged

    fun hasErrors(): Boolean = errors.isNotEmpty()
}

