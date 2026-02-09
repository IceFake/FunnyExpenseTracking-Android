package com.example.funnyexpensetracking.domain.model

import com.example.funnyexpensetracking.data.local.entity.*

/**
 * 备份数据模型
 * 用于导出和导入所有用户数据
 */
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val transactions: List<TransactionBackup> = emptyList(),
    val accounts: List<AccountBackup> = emptyList(),
    val fixedIncomes: List<FixedIncomeBackup> = emptyList(),
    val investments: List<InvestmentBackup> = emptyList(),
    val stockHoldings: List<StockHoldingBackup> = emptyList(),
    val assetBaseline: AssetBaselineBackup? = null,
    val assetSnapshots: List<AssetSnapshotBackup> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * 交易记录备份（不包含ID，用于导入时创建新记录）
 * 使用String存储枚举类型以确保序列化/反序列化兼容性
 */
data class TransactionBackup(
    val amount: Double,
    val type: String,  // TransactionType的名称
    val category: String,
    val accountName: String,  // 使用账户名称而非ID，方便跨设备导入
    val note: String,
    val date: Long,
    val createdAt: Long
)

/**
 * 账户备份
 */
data class AccountBackup(
    val name: String,
    val icon: String,
    val balance: Double,
    val isDefault: Boolean,
    val sortOrder: Int,
    val createdAt: Long
)

/**
 * 固定收支备份
 */
data class FixedIncomeBackup(
    val name: String,
    val amount: Double,
    val type: String,  // FixedIncomeType的名称
    val frequency: String,  // FixedIncomeFrequency的名称
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean,
    val accumulatedAmount: Double,
    val createdAt: Long
)

/**
 * 投资备份
 */
data class InvestmentBackup(
    val category: String,  // InvestmentCategory的名称
    val description: String,
    val quantity: Double,
    val investment: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val createdAt: Long
)

/**
 * 股票持仓备份
 */
data class StockHoldingBackup(
    val symbol: String,
    val name: String,
    val shares: Double,
    val purchasePrice: Double,          // 平均购入单价
    val totalPurchaseCost: Double,      // 购入总价（用于合并时叠加）
    val purchaseDate: Long,
    val currentPrice: Double,
    val createdAt: Long
)

/**
 * 资产基准值备份
 */
data class AssetBaselineBackup(
    val baselineTimestamp: Long,
    val baselineAmount: Double
)

/**
 * 资产快照备份
 */
data class AssetSnapshotBackup(
    val totalAsset: Double,
    val cashAsset: Double,
    val stockAsset: Double,
    val timestamp: Long
)

/**
 * 扩展函数：将实体转换为备份模型
 */
fun TransactionEntity.toBackup(accountName: String) = TransactionBackup(
    amount = amount,
    type = type.name,
    category = category,
    accountName = accountName,
    note = note,
    date = date,
    createdAt = createdAt
)

fun AccountEntity.toBackup() = AccountBackup(
    name = name,
    icon = icon,
    balance = balance,
    isDefault = isDefault,
    sortOrder = sortOrder,
    createdAt = createdAt
)

fun FixedIncomeEntity.toBackup() = FixedIncomeBackup(
    name = name,
    amount = amount,
    type = type.name,
    frequency = frequency.name,
    startDate = startDate,
    endDate = endDate,
    isActive = isActive,
    accumulatedAmount = accumulatedAmount,
    createdAt = createdAt
)

fun InvestmentEntity.toBackup() = InvestmentBackup(
    category = category.name,
    description = description,
    quantity = quantity,
    investment = investment,
    currentPrice = currentPrice,
    currentValue = currentValue,
    createdAt = createdAt
)

fun StockHoldingEntity.toBackup() = StockHoldingBackup(
    symbol = symbol,
    name = name,
    shares = shares,
    purchasePrice = purchasePrice,
    totalPurchaseCost = totalCost,  // 直接使用totalCost字段，避免计算误差
    purchaseDate = purchaseDate,
    currentPrice = currentPrice,
    createdAt = createdAt
)

fun AssetBaselineEntity.toBackup() = AssetBaselineBackup(
    baselineTimestamp = baselineTimestamp,
    baselineAmount = baselineAmount
)

fun AssetSnapshotEntity.toBackup() = AssetSnapshotBackup(
    totalAsset = totalAsset,
    cashAsset = cashAsset,
    stockAsset = stockAsset,
    timestamp = timestamp
)

