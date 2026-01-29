package com.example.funnyexpensetracking.ui.transaction

import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 交易记录页面状态
 */
data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val loadingState: LoadingState = LoadingState.IDLE,
    val errorMessage: String? = null,
    val incomeCategories: List<String> = listOf("工资", "奖金", "投资收益", "兼职", "其他"),
    val expenseCategories: List<String> = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "教育", "其他"),
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0
) : UiState

/**
 * 交易记录页面事件
 */
sealed class TransactionUiEvent : UiEvent {
    data class ShowMessage(val message: String) : TransactionUiEvent()
    data class NavigateToDetail(val transactionId: Long) : TransactionUiEvent()
    object TransactionAdded : TransactionUiEvent()
    object TransactionDeleted : TransactionUiEvent()
}

