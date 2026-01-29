package com.example.funnyexpensetracking.ui.fixedincome

import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 固定收支筛选类型
 */
enum class FixedIncomeFilterType {
    ALL,        // 全部
    INCOME,     // 仅收入
    EXPENSE     // 仅支出
}

/**
 * 固定收支页面状态
 */
data class FixedIncomeUiState(
    // 所有固定收支
    val allFixedIncomes: List<FixedIncome> = emptyList(),
    // 筛选后的列表
    val filteredFixedIncomes: List<FixedIncome> = emptyList(),
    // 当前筛选类型
    val filterType: FixedIncomeFilterType = FixedIncomeFilterType.ALL,
    // 每分钟总收入
    val incomePerMinute: Double = 0.0,
    // 每分钟总支出
    val expensePerMinute: Double = 0.0,
    // 每分钟净变动
    val netPerMinute: Double = 0.0,
    // 加载状态
    val loadingState: LoadingState = LoadingState.IDLE,
    // 是否显示添加弹窗
    val showAddDialog: Boolean = false,
    // 当前编辑的固定收支
    val editingFixedIncome: FixedIncome? = null
) : UiState

/**
 * 固定收支页面事件
 */
sealed class FixedIncomeUiEvent : UiEvent {
    data class ShowMessage(val message: String) : FixedIncomeUiEvent()
    object FixedIncomeAdded : FixedIncomeUiEvent()
    object FixedIncomeUpdated : FixedIncomeUiEvent()
    object FixedIncomeDeleted : FixedIncomeUiEvent()
    object DismissDialog : FixedIncomeUiEvent()
}

