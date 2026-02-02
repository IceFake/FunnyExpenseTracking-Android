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
    // 当天的交易记录列表
    val transactions: List<com.example.funnyexpensetracking.domain.model.Transaction> = emptyList(),
    // 当前选中的日期时间戳
    val selectedDate: Long = 0L,
    // 当天收入
    val dayIncome: Double = 0.0,
    // 当天支出
    val dayExpense: Double = 0.0,
    // 当天结余
    val dayBalance: Double = 0.0,
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

