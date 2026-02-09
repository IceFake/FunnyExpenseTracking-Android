package com.example.funnyexpensetracking.ui.investment

import com.example.funnyexpensetracking.domain.model.Investment
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 投资筛选类型
 */
enum class InvestmentFilterType {
    ALL,        // 全部
    STOCK,      // 股票
    OTHER       // 其他
}

/**
 * 投资页面状态
 */
data class InvestmentUiState(
    // 所有投资条目
    val allInvestments: List<Investment> = emptyList(),
    // 筛选后的列表
    val filteredInvestments: List<Investment> = emptyList(),
    // 当前筛选类型
    val filterType: InvestmentFilterType = InvestmentFilterType.ALL,
    // 总投入
    val totalInvestment: Double = 0.0,
    // 总当前价值
    val totalCurrentValue: Double = 0.0,
    // 总盈亏
    val totalProfitLoss: Double = 0.0,
    // 加载状态
    val loadingState: LoadingState = LoadingState.IDLE,
    // 是否显示添加弹窗
    val showAddDialog: Boolean = false,
    // 当前编辑的投资条目
    val editingInvestment: Investment? = null,
    // 是否正在刷新股票价格
    val isRefreshing: Boolean = false
) : UiState

/**
 * 投资页面事件
 */
sealed class InvestmentUiEvent : UiEvent {
    data class ShowMessage(val message: String) : InvestmentUiEvent()
    object InvestmentAdded : InvestmentUiEvent()
    object InvestmentUpdated : InvestmentUiEvent()
    object InvestmentDeleted : InvestmentUiEvent()
    object DismissDialog : InvestmentUiEvent()
}

