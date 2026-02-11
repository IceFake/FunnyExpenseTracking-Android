package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.*
import com.example.funnyexpensetracking.data.local.entity.AccountEntity
import com.example.funnyexpensetracking.data.local.entity.AssetBaselineEntity
import com.example.funnyexpensetracking.data.local.entity.AssetSnapshotEntity
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeEntity
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType
import com.example.funnyexpensetracking.data.local.entity.InvestmentCategory
import com.example.funnyexpensetracking.data.local.entity.InvestmentEntity
import com.example.funnyexpensetracking.data.local.entity.StockHoldingEntity
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.local.entity.TransactionType
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.DataManagementRepository
import com.example.funnyexpensetracking.domain.repository.ImportResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据管理Repository实现
 */
@Singleton
class DataManagementRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val fixedIncomeDao: FixedIncomeDao,
    private val investmentDao: InvestmentDao,
    private val stockHoldingDao: StockHoldingDao,
    private val assetBaselineDao: AssetBaselineDao,
    private val assetSnapshotDao: AssetSnapshotDao
) : DataManagementRepository {

    override suspend fun exportAllData(): BackupData {
        // 获取所有账户，用于将账户ID映射为账户名称
        val accounts = accountDao.getAllAccounts().first()
        val accountIdToName = accounts.associate { it.id to it.name }

        // 获取所有交易记录
        val transactions = transactionDao.getAllTransactions().first()
        val transactionBackups = transactions.map { transaction ->
            transaction.toBackup(accountIdToName[transaction.accountId] ?: "未知账户")
        }

        // 获取固定收支
        val fixedIncomes = fixedIncomeDao.getAllFixedIncomes().first()

        // 获取投资记录
        val investments = investmentDao.getAllInvestments().first()

        // 获取股票持仓
        val stockHoldings = stockHoldingDao.getAllStockHoldings().first()

        // 获取资产基准值
        val assetBaseline = assetBaselineDao.getBaseline()

        // 获取资产快照（最近100条）
        val assetSnapshots = assetSnapshotDao.getRecentSnapshots(100).first()

        return BackupData(
            transactions = transactionBackups,
            accounts = accounts.map { it.toBackup() },
            fixedIncomes = fixedIncomes.map { it.toBackup() },
            investments = investments.map { it.toBackup() },
            stockHoldings = stockHoldings.map { it.toBackup() },
            assetBaseline = assetBaseline?.toBackup(),
            assetSnapshots = assetSnapshots.map { it.toBackup() }
        )
    }

    override suspend fun importData(backupData: BackupData): ImportResult {
        val errors = mutableListOf<String>()
        var transactionsAdded = 0
        var accountsAdded = 0
        var accountsMerged = 0
        var fixedIncomesAdded = 0
        var fixedIncomesMerged = 0
        var investmentsAdded = 0
        var investmentsMerged = 0
        var stockHoldingsAdded = 0
        var stockHoldingsMerged = 0
        var assetSnapshotsAdded = 0

        try {
            // 1. 导入账户（按名称匹配，相同名称叠加余额）
            val existingAccounts = accountDao.getAllAccounts().first()
            val accountNameToId = mutableMapOf<String, Long>()

            for (accountBackup in backupData.accounts) {
                val existingAccount = existingAccounts.find { it.name == accountBackup.name }
                if (existingAccount != null) {
                    // 叠加余额
                    accountDao.setBalance(
                        accountId = existingAccount.id,
                        balance = existingAccount.balance + accountBackup.balance
                    )
                    accountNameToId[accountBackup.name] = existingAccount.id
                    accountsMerged++
                } else {
                    // 新增账户
                    val newAccountId = accountDao.insert(
                        AccountEntity(
                            name = accountBackup.name,
                            icon = accountBackup.icon,
                            balance = accountBackup.balance,
                            isDefault = false, // 导入时不设为默认
                            sortOrder = accountBackup.sortOrder,
                            createdAt = accountBackup.createdAt
                        )
                    )
                    accountNameToId[accountBackup.name] = newAccountId
                    accountsAdded++
                }
            }

            // 刷新账户映射（包含原有账户）
            val allAccounts = accountDao.getAllAccounts().first()
            allAccounts.forEach { accountNameToId[it.name] = it.id }

            // 2. 导入交易记录（全部新增，不去重）
            for (transactionBackup in backupData.transactions) {
                val accountId = accountNameToId[transactionBackup.accountName]
                val transactionType = try {
                    TransactionType.valueOf(transactionBackup.type)
                } catch (e: Exception) {
                    TransactionType.EXPENSE // 默认为支出
                }

                if (accountId != null) {
                    transactionDao.insert(
                        TransactionEntity(
                            amount = transactionBackup.amount,
                            type = transactionType,
                            category = transactionBackup.category,
                            accountId = accountId,
                            note = transactionBackup.note,
                            date = transactionBackup.date,
                            createdAt = transactionBackup.createdAt,
                            syncStatus = SyncStatus.PENDING_UPLOAD
                        )
                    )
                    transactionsAdded++
                } else {
                    // 创建默认账户来存放无法匹配的交易
                    val defaultAccountId = getOrCreateDefaultAccount()
                    transactionDao.insert(
                        TransactionEntity(
                            amount = transactionBackup.amount,
                            type = transactionType,
                            category = transactionBackup.category,
                            accountId = defaultAccountId,
                            note = transactionBackup.note + " [原账户: ${transactionBackup.accountName}]",
                            date = transactionBackup.date,
                            createdAt = transactionBackup.createdAt,
                            syncStatus = SyncStatus.PENDING_UPLOAD
                        )
                    )
                    transactionsAdded++
                    errors.add("交易记录的账户'${transactionBackup.accountName}'不存在，已使用默认账户")
                }
            }

            // 3. 导入固定收支（按名称和类型匹配，相同则叠加累计时间和金额）
            val existingFixedIncomes = fixedIncomeDao.getAllFixedIncomes().first()
            for (fiBackup in backupData.fixedIncomes) {
                val fiType = try {
                    FixedIncomeType.valueOf(fiBackup.type)
                } catch (e: Exception) {
                    FixedIncomeType.EXPENSE
                }
                val fiFrequency = try {
                    FixedIncomeFrequency.valueOf(fiBackup.frequency)
                } catch (e: Exception) {
                    FixedIncomeFrequency.MONTHLY
                }

                val existing = existingFixedIncomes.find {
                    it.name == fiBackup.name && it.type.name == fiBackup.type
                }
                if (existing != null) {
                    // 叠加累计时间和重新计算累计金额
                    val newAccumulatedMinutes = existing.accumulatedMinutes + fiBackup.accumulatedMinutes
                    val newAccumulatedAmount = fiFrequency.calculateAccumulatedAmount(existing.amount, newAccumulatedMinutes)
                    fixedIncomeDao.updateAccumulated(
                        id = existing.id,
                        accumulatedMinutes = newAccumulatedMinutes,
                        accumulatedAmount = newAccumulatedAmount,
                        lastRecordTime = System.currentTimeMillis()
                    )
                    fixedIncomesMerged++
                } else {
                    // 新增
                    fixedIncomeDao.insert(
                        FixedIncomeEntity(
                            name = fiBackup.name,
                            amount = fiBackup.amount,
                            type = fiType,
                            frequency = fiFrequency,
                            startDate = fiBackup.startDate,
                            endDate = fiBackup.endDate,
                            isActive = fiBackup.isActive,
                            accumulatedMinutes = fiBackup.accumulatedMinutes,
                            accumulatedAmount = fiBackup.accumulatedAmount,
                            lastRecordTime = fiBackup.lastRecordTime,
                            createdAt = fiBackup.createdAt
                        )
                    )
                    fixedIncomesAdded++
                }
            }

            // 4. 导入投资（按描述和分类匹配，相同则叠加数量和投入金额）
            val existingInvestments = investmentDao.getAllInvestments().first()
            for (invBackup in backupData.investments) {
                val invCategory = try {
                    InvestmentCategory.valueOf(invBackup.category)
                } catch (e: Exception) {
                    InvestmentCategory.OTHER
                }

                val existing = existingInvestments.find {
                    it.description == invBackup.description && it.category.name == invBackup.category
                }
                if (existing != null) {
                    // 叠加数量和投入金额
                    investmentDao.update(
                        existing.copy(
                            quantity = existing.quantity + invBackup.quantity,
                            investment = existing.investment + invBackup.investment,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    investmentsMerged++
                } else {
                    // 新增
                    investmentDao.insert(
                        InvestmentEntity(
                            category = invCategory,
                            description = invBackup.description,
                            quantity = invBackup.quantity,
                            investment = invBackup.investment,
                            currentPrice = invBackup.currentPrice,
                            currentValue = invBackup.currentValue,
                            createdAt = invBackup.createdAt
                        )
                    )
                    investmentsAdded++
                }
            }

            // 5. 导入股票持仓（按股票代码匹配，相同则叠加股数和购入总价）
            val existingStockHoldings = stockHoldingDao.getAllStockHoldings().first()
            for (shBackup in backupData.stockHoldings) {
                val existing = existingStockHoldings.find { it.symbol == shBackup.symbol }
                if (existing != null) {
                    // 叠加股数和购入总价
                    val totalShares = existing.shares + shBackup.shares
                    // 直接使用totalCost字段叠加，避免单价计算误差
                    val newTotalCost = existing.totalCost + shBackup.totalPurchaseCost
                    // 计算新的平均购入价格（仅用于展示）
                    val newAvgPrice = if (totalShares > 0) newTotalCost / totalShares else existing.purchasePrice

                    stockHoldingDao.update(
                        existing.copy(
                            shares = totalShares,
                            purchasePrice = newAvgPrice,
                            totalCost = newTotalCost,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                    stockHoldingsMerged++
                } else {
                    // 新增
                    stockHoldingDao.insert(
                        StockHoldingEntity(
                            symbol = shBackup.symbol,
                            name = shBackup.name,
                            shares = shBackup.shares,
                            purchasePrice = shBackup.purchasePrice,
                            totalCost = shBackup.totalPurchaseCost,
                            purchaseDate = shBackup.purchaseDate,
                            currentPrice = shBackup.currentPrice,
                            createdAt = shBackup.createdAt
                        )
                    )
                    stockHoldingsAdded++
                }
            }

            // 6. 导入资产基准值（如果当前没有，则导入；否则叠加金额）
            backupData.assetBaseline?.let { baselineBackup ->
                val existingBaseline = assetBaselineDao.getBaseline()
                if (existingBaseline != null) {
                    assetBaselineDao.updateBaseline(
                        amount = existingBaseline.baselineAmount + baselineBackup.baselineAmount,
                        timestamp = existingBaseline.baselineTimestamp
                    )
                } else {
                    assetBaselineDao.insert(
                        AssetBaselineEntity(
                            baselineTimestamp = baselineBackup.baselineTimestamp,
                            baselineAmount = baselineBackup.baselineAmount
                        )
                    )
                }
            }

            // 7. 导入资产快照（检查时间戳，不重复导入相同时间戳的快照）
            val existingSnapshots = assetSnapshotDao.getRecentSnapshots(1000).first()
            val existingTimestamps = existingSnapshots.map { it.timestamp }.toSet()
            for (snapshotBackup in backupData.assetSnapshots) {
                if (snapshotBackup.timestamp !in existingTimestamps) {
                    assetSnapshotDao.insert(
                        AssetSnapshotEntity(
                            totalAsset = snapshotBackup.totalAsset,
                            cashAsset = snapshotBackup.cashAsset,
                            stockAsset = snapshotBackup.stockAsset,
                            timestamp = snapshotBackup.timestamp
                        )
                    )
                    assetSnapshotsAdded++
                }
            }

        } catch (e: Exception) {
            errors.add("导入过程发生错误: ${e.message}")
        }

        return ImportResult(
            transactionsAdded = transactionsAdded,
            accountsAdded = accountsAdded,
            accountsMerged = accountsMerged,
            fixedIncomesAdded = fixedIncomesAdded,
            fixedIncomesMerged = fixedIncomesMerged,
            investmentsAdded = investmentsAdded,
            investmentsMerged = investmentsMerged,
            stockHoldingsAdded = stockHoldingsAdded,
            stockHoldingsMerged = stockHoldingsMerged,
            assetSnapshotsAdded = assetSnapshotsAdded,
            errors = errors
        )
    }

    override suspend fun clearAllData() {
        transactionDao.deleteAll()
        accountDao.deleteAll()
        fixedIncomeDao.deleteAll()
        investmentDao.deleteAll()
        stockHoldingDao.deleteAll()
        assetBaselineDao.deleteAll()
        assetSnapshotDao.deleteAll()
    }

    /**
     * 获取或创建默认账户
     */
    private suspend fun getOrCreateDefaultAccount(): Long {
        val defaultAccount = accountDao.getDefaultAccount()
        if (defaultAccount != null) {
            return defaultAccount.id
        }

        // 获取第一个账户
        val accounts = accountDao.getAllAccounts().first()
        if (accounts.isNotEmpty()) {
            return accounts.first().id
        }

        // 创建新的默认账户
        return accountDao.insert(
            AccountEntity(
                name = "默认账户",
                icon = "",
                balance = 0.0,
                isDefault = true,
                sortOrder = 0
            )
        )
    }
}

