package com.example.funnyexpensetracking.ui.history

import com.example.funnyexpensetracking.domain.model.DailyTransactions
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 历史账单页面状态
 */
data class HistoryUiState(
    // 交易记录列表（按日期分组）
    val dailyTransactions: List<DailyTransactions> = emptyList(),
    // 当前选中年份
    val selectedYear: Int = 0,
    // 当前选中月份
    val selectedMonth: Int = 0,
    // 本月收入
    val monthIncome: Double = 0.0,
    // 本月支出
    val monthExpense: Double = 0.0,
    // 本月结余
    val monthBalance: Double = 0.0,
    // 加载状态
    val loadingState: LoadingState = LoadingState.IDLE
) : UiState

/**
 * 历史账单页面事件
 */
sealed class HistoryUiEvent : UiEvent {
    data class ShowMessage(val message: String) : HistoryUiEvent()
    data class NavigateToDetail(val transactionId: Long) : HistoryUiEvent()
    object TransactionDeleted : HistoryUiEvent()
}

