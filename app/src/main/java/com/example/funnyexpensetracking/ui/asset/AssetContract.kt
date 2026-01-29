package com.example.funnyexpensetracking.ui.asset

import com.example.funnyexpensetracking.domain.model.AssetSummary
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.StockHolding
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 资产页面状态
 */
data class AssetUiState(
    val assetSummary: AssetSummary? = null,
    val fixedIncomes: List<FixedIncome> = emptyList(),
    val stockHoldings: List<StockHolding> = emptyList(),
    val loadingState: LoadingState = LoadingState.IDLE,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    // 实时资产显示（每分钟更新）
    val realtimeTotalAsset: Double = 0.0,
    val lastUpdateTime: Long = 0
) : UiState

/**
 * 资产页面事件
 */
sealed class AssetUiEvent : UiEvent {
    data class ShowMessage(val message: String) : AssetUiEvent()
    object FixedIncomeAdded : AssetUiEvent()
    object StockAdded : AssetUiEvent()
    data class NavigateToStockDetail(val symbol: String) : AssetUiEvent()
}

