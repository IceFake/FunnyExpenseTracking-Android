package com.example.funnyexpensetracking.ui.calendar

import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 日历页面状态
 */
data class CalendarUiState(
    // 日历数据，key为日期时间戳，value为当日收支差额
    val dailyBalanceMap: Map<Long, Double> = emptyMap(),
    // 当前选中的年份
    val selectedYear: Int = 0,
    // 当前选中的月份
    val selectedMonth: Int = 0,
    // 本月总收入
    val monthIncome: Double = 0.0,
    // 本月总支出
    val monthExpense: Double = 0.0,
    // 本月结余
    val monthBalance: Double = 0.0,
    // 加载状态
    val loadingState: LoadingState = LoadingState.IDLE
) : UiState

/**
 * 日历页面事件
 */
sealed class CalendarUiEvent : UiEvent {
    data class ShowMessage(val message: String) : CalendarUiEvent()
    data class NavigateToDay(val year: Int, val month: Int, val day: Int) : CalendarUiEvent()
}

