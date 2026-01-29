package com.example.funnyexpensetracking.domain.model

/**
 * 交易类型
 */
enum class TransactionType {
    INCOME,
    EXPENSE
}

/**
 * 交易记录领域模型
 */
data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val accountId: Long,
    val accountName: String = "",          // 账户名称（用于显示）
    val note: String = "",
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 按日期分组的交易记录
 */
data class DailyTransactions(
    val date: Long,                        // 日期（年月日的时间戳）
    val dateString: String,                // 格式化的日期字符串
    val dayOfWeek: String,                 // 星期几
    val totalIncome: Double,               // 当日收入
    val totalExpense: Double,              // 当日支出
    val transactions: List<Transaction>    // 当日交易列表
)

/**
 * 账户模型
 */
data class Account(
    val id: Long = 0,
    val name: String,
    val icon: String = "",
    val balance: Double = 0.0,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

