package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.*
import com.example.funnyexpensetracking.data.local.entity.*
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.AssetRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType as EntityFixedIncomeType
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency as EntityFixedIncomeFrequency
import com.example.funnyexpensetracking.data.local.entity.TransactionType as EntityTransactionType

/**
 * 资产管理Repository实现类
 */
@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val fixedIncomeDao: FixedIncomeDao,
    private val assetSnapshotDao: AssetSnapshotDao,
    private val transactionDao: TransactionDao,
    private val stockHoldingDao: StockHoldingDao,
    private val investmentDao: InvestmentDao
) : AssetRepository {

    override fun getAllActiveFixedIncomes(): Flow<List<FixedIncome>> {
        return fixedIncomeDao.getAllActiveFixedIncomes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllFixedIncomes(): Flow<List<FixedIncome>> {
        return fixedIncomeDao.getAllFixedIncomes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFixedIncomesByType(type: FixedIncomeType): Flow<List<FixedIncome>> {
        return fixedIncomeDao.getActiveByType(type.toEntityType()).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun addFixedIncome(fixedIncome: FixedIncome): Long {
        return fixedIncomeDao.insert(fixedIncome.toEntity())
    }

    override suspend fun updateFixedIncome(fixedIncome: FixedIncome) {
        fixedIncomeDao.update(fixedIncome.toEntity())
    }

    override suspend fun deleteFixedIncome(fixedIncome: FixedIncome) {
        fixedIncomeDao.delete(fixedIncome.toEntity())
    }

    override suspend fun updateFixedIncomeStatus(id: Long, isActive: Boolean) {
        fixedIncomeDao.updateActiveStatus(id, isActive)
    }

    override suspend fun saveAssetSnapshot(snapshot: AssetSnapshot): Long {
        return assetSnapshotDao.insert(snapshot.toEntity())
    }

    override suspend fun getLatestSnapshot(): AssetSnapshot? {
        return assetSnapshotDao.getLatestSnapshot()?.toDomainModel()
    }

    override fun getLatestSnapshotFlow(): Flow<AssetSnapshot?> {
        return assetSnapshotDao.getLatestSnapshotFlow().map { it?.toDomainModel() }
    }

    override fun getSnapshotsByTimeRange(startTime: Long, endTime: Long): Flow<List<AssetSnapshot>> {
        return assetSnapshotDao.getSnapshotsByTimeRange(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRecentSnapshots(limit: Int): Flow<List<AssetSnapshot>> {
        return assetSnapshotDao.getRecentSnapshots(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun calculateCurrentAssetSummary(): AssetSummary {
        val currentTime = System.currentTimeMillis()

        val totalIncome = transactionDao.getTotalByTypeAndDateRange(
            EntityTransactionType.INCOME, 0, currentTime
        ) ?: 0.0
        val totalExpense = transactionDao.getTotalByTypeAndDateRange(
            EntityTransactionType.EXPENSE, 0, currentTime
        ) ?: 0.0

        val fixedIncomes = fixedIncomeDao.getAllActiveFixedIncomes().first()
        var fixedIncomePerMinute = 0.0
        var fixedExpensePerMinute = 0.0

        fixedIncomes.forEach { entity ->
            val amountPerMinute = calculateAmountPerMinute(entity.amount, entity.frequency)
            if (entity.type == EntityFixedIncomeType.INCOME) {
                fixedIncomePerMinute += amountPerMinute
            } else {
                fixedExpensePerMinute += amountPerMinute
            }
        }

        // 股票持仓（资产管理模块）
        val stockValue = stockHoldingDao.getTotalStockValue() ?: 0.0
        val stockCost = stockHoldingDao.getTotalStockCost() ?: 0.0

        // 投资/理财模块的价值
        val investmentCurrentValue = investmentDao.getTotalCurrentValue()
        val investmentCost = investmentDao.getTotalInvestment()

        // 合并股票和投资的价值
        val totalStockAsset = stockValue + investmentCurrentValue
        val totalStockCost = stockCost + investmentCost
        val stockProfitLoss = totalStockAsset - totalStockCost

        val cashAsset = totalIncome - totalExpense

        return AssetSummary(
            totalAsset = cashAsset + totalStockAsset,
            cashAsset = cashAsset,
            stockAsset = totalStockAsset,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            fixedIncomePerMinute = fixedIncomePerMinute,
            fixedExpensePerMinute = fixedExpensePerMinute,
            netIncomePerMinute = fixedIncomePerMinute - fixedExpensePerMinute,
            stockProfitLoss = stockProfitLoss,
            timestamp = currentTime
        )
    }

    override fun getAssetSummaryFlow(): Flow<AssetSummary> {
        return flow {
            while (true) {
                emit(calculateCurrentAssetSummary())
                delay(60_000)
            }
        }
    }

    private fun calculateAmountPerMinute(amount: Double, frequency: EntityFixedIncomeFrequency): Double {
        return when (frequency) {
            EntityFixedIncomeFrequency.DAILY -> amount / (24 * 60)
            EntityFixedIncomeFrequency.WEEKLY -> amount / (7 * 24 * 60)
            EntityFixedIncomeFrequency.MONTHLY -> amount / (30 * 24 * 60)
            EntityFixedIncomeFrequency.YEARLY -> amount / (365 * 24 * 60)
        }
    }

    private fun FixedIncomeEntity.toDomainModel(): FixedIncome {
        return FixedIncome(
            id = id,
            name = name,
            amount = amount,
            type = type.toDomainType(),
            frequency = frequency.toDomainFrequency(),
            startDate = startDate,
            endDate = endDate,
            isActive = isActive
        )
    }

    private fun FixedIncome.toEntity(): FixedIncomeEntity {
        return FixedIncomeEntity(
            id = id,
            name = name,
            amount = amount,
            type = type.toEntityType(),
            frequency = frequency.toEntityFrequency(),
            startDate = startDate,
            endDate = endDate,
            isActive = isActive
        )
    }

    private fun AssetSnapshotEntity.toDomainModel(): AssetSnapshot {
        return AssetSnapshot(id = id, totalAsset = totalAsset, cashAsset = cashAsset, stockAsset = stockAsset, timestamp = timestamp)
    }

    private fun AssetSnapshot.toEntity(): AssetSnapshotEntity {
        return AssetSnapshotEntity(id = id, totalAsset = totalAsset, cashAsset = cashAsset, stockAsset = stockAsset, timestamp = timestamp)
    }

    private fun EntityFixedIncomeType.toDomainType(): FixedIncomeType {
        return when (this) {
            EntityFixedIncomeType.INCOME -> FixedIncomeType.INCOME
            EntityFixedIncomeType.EXPENSE -> FixedIncomeType.EXPENSE
        }
    }

    private fun FixedIncomeType.toEntityType(): EntityFixedIncomeType {
        return when (this) {
            FixedIncomeType.INCOME -> EntityFixedIncomeType.INCOME
            FixedIncomeType.EXPENSE -> EntityFixedIncomeType.EXPENSE
        }
    }

    private fun EntityFixedIncomeFrequency.toDomainFrequency(): FixedIncomeFrequency {
        return when (this) {
            EntityFixedIncomeFrequency.DAILY -> FixedIncomeFrequency.DAILY
            EntityFixedIncomeFrequency.WEEKLY -> FixedIncomeFrequency.WEEKLY
            EntityFixedIncomeFrequency.MONTHLY -> FixedIncomeFrequency.MONTHLY
            EntityFixedIncomeFrequency.YEARLY -> FixedIncomeFrequency.YEARLY
        }
    }

    private fun FixedIncomeFrequency.toEntityFrequency(): EntityFixedIncomeFrequency {
        return when (this) {
            FixedIncomeFrequency.DAILY -> EntityFixedIncomeFrequency.DAILY
            FixedIncomeFrequency.WEEKLY -> EntityFixedIncomeFrequency.WEEKLY
            FixedIncomeFrequency.MONTHLY -> EntityFixedIncomeFrequency.MONTHLY
            FixedIncomeFrequency.YEARLY -> EntityFixedIncomeFrequency.YEARLY
        }
    }
}

