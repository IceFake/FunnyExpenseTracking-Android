package com.example.funnyexpensetracking.ui.transaction

import com.example.funnyexpensetracking.domain.model.Account
import com.example.funnyexpensetracking.domain.model.DailyTransactions
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 收入分类
 */
val INCOME_CATEGORIES = listOf("工资", "奖金", "投资收益", "兼职", "红包", "退款", "其他收入")

/**
 * 支出分类
 */
val EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "教育", "居住", "通讯", "服饰", "其他支出")

/**
 * 交易记录页面状态
 */
data class TransactionUiState(
    // 交易记录列表（按日期分组）
    val dailyTransactions: List<DailyTransactions> = emptyList(),
    // 所有交易记录
    val transactions: List<Transaction> = emptyList(),
    // 账户列表
    val accounts: List<Account> = emptyList(),
    // 当前选中日期
    val selectedDate: Long = System.currentTimeMillis(),
    // 加载状态
    val loadingState: LoadingState = LoadingState.IDLE,
    // 错误信息
    val errorMessage: String? = null,
    // 今日收入
    val todayIncome: Double = 0.0,
    // 今日支出
    val todayExpense: Double = 0.0,
    // 总余额
    val totalBalance: Double = 0.0,
    // 是否显示添加交易对话框
    val showAddDialog: Boolean = false,
    // 是否显示添加账户对话框
    val showAddAccountDialog: Boolean = false,
    // 当前编辑的交易（用于编辑模式）
    val editingTransaction: Transaction? = null,
    // ========== 同步状态 ==========
    // 是否正在同步
    val isSyncing: Boolean = false,
    // 是否处于离线状态
    val isOffline: Boolean = false,
    // 待同步的记录数量
    val pendingSyncCount: Int = 0
) : UiState

/**
 * 添加交易表单状态
 */
data class AddTransactionFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val category: String = "",
    val selectedAccount: Account? = null,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isValid: Boolean = false
)

/**
 * 交易记录页面事件
 */
sealed class TransactionUiEvent : UiEvent {
    data class ShowMessage(val message: String) : TransactionUiEvent()
    data class NavigateToDetail(val transactionId: Long) : TransactionUiEvent()
    object TransactionAdded : TransactionUiEvent()
    object TransactionDeleted : TransactionUiEvent()
    object TransactionUpdated : TransactionUiEvent()
    object AccountAdded : TransactionUiEvent()
    object DismissDialog : TransactionUiEvent()
}

