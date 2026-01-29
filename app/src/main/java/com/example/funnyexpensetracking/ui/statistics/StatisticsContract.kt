package com.example.funnyexpensetracking.ui.statistics

import com.example.funnyexpensetracking.domain.model.CategoryStat
import com.example.funnyexpensetracking.domain.model.Statistics
import com.example.funnyexpensetracking.domain.model.TrendStatistics
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 统计页面状态
 */
data class StatisticsUiState(
    val currentStatistics: Statistics? = null,
    val categoryStats: List<CategoryStat> = emptyList(),
    val trendStatistics: TrendStatistics? = null,
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    val isMonthlyView: Boolean = true,  // true: 月视图, false: 年视图
    val loadingState: LoadingState = LoadingState.IDLE,
    val chartUrl: String? = null,
    val errorMessage: String? = null
) : UiState

/**
 * 统计页面事件
 */
sealed class StatisticsUiEvent : UiEvent {
    data class ShowMessage(val message: String) : StatisticsUiEvent()
    data class OpenChart(val url: String) : StatisticsUiEvent()
}

