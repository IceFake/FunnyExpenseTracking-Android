package com.example.funnyexpensetracking.ui.statistics

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.repository.StatisticsRepository
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.DateTimeUtil
import com.example.funnyexpensetracking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统计页面ViewModel
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : BaseViewModel<StatisticsUiState, StatisticsUiEvent>() {

    override fun initialState() = StatisticsUiState(
        selectedYear = DateTimeUtil.getCurrentYear(),
        selectedMonth = DateTimeUtil.getCurrentMonth()
    )

    init {
        loadCurrentMonthStatistics()
    }

    /**
     * 加载当前月份统计
     */
    fun loadCurrentMonthStatistics() {
        val year = currentState().selectedYear
        val month = currentState().selectedMonth
        loadMonthlyStatistics(year, month)
    }

    /**
     * 加载月度统计
     */
    fun loadMonthlyStatistics(year: Int, month: Int) {
        updateState {
            copy(
                loadingState = LoadingState.LOADING,
                selectedYear = year,
                selectedMonth = month,
                isMonthlyView = true
            )
        }

        viewModelScope.launch {
            when (val result = statisticsRepository.getMonthlyStatistics(year, month)) {
                is Resource.Success -> {
                    updateState {
                        copy(
                            currentStatistics = result.data,
                            chartUrl = result.data?.chartUrl,
                            loadingState = LoadingState.SUCCESS
                        )
                    }
                }
                is Resource.Error -> {
                    updateState {
                        copy(
                            loadingState = LoadingState.ERROR,
                            errorMessage = result.message
                        )
                    }
                    sendEvent(StatisticsUiEvent.ShowMessage("加载失败: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
            loadCategoryStatistics(year, month)
        }
    }

    /**
     * 加载年度统计
     */
    fun loadYearlyStatistics(year: Int) {
        updateState {
            copy(
                loadingState = LoadingState.LOADING,
                selectedYear = year,
                isMonthlyView = false
            )
        }

        viewModelScope.launch {
            when (val result = statisticsRepository.getYearlyStatistics(year)) {
                is Resource.Success -> {
                    updateState {
                        copy(
                            currentStatistics = result.data,
                            chartUrl = result.data?.chartUrl,
                            loadingState = LoadingState.SUCCESS
                        )
                    }
                }
                is Resource.Error -> {
                    updateState {
                        copy(
                            loadingState = LoadingState.ERROR,
                            errorMessage = result.message
                        )
                    }
                    sendEvent(StatisticsUiEvent.ShowMessage("加载失败: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 加载分类统计
     */
    private fun loadCategoryStatistics(year: Int, month: Int) {
        viewModelScope.launch {
            when (val result = statisticsRepository.getCategoryStatistics(year, month)) {
                is Resource.Success -> {
                    updateState { copy(categoryStats = result.data ?: emptyList()) }
                }
                is Resource.Error -> {}
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 加载趋势统计
     */
    fun loadTrendStatistics(months: Int = 6) {
        viewModelScope.launch {
            when (val result = statisticsRepository.getTrendStatistics(months)) {
                is Resource.Success -> {
                    updateState { copy(trendStatistics = result.data) }
                }
                is Resource.Error -> {
                    sendEvent(StatisticsUiEvent.ShowMessage("加载趋势失败"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 切换到上个月
     */
    fun previousMonth() {
        val state = currentState()
        var year = state.selectedYear
        var month = state.selectedMonth - 1
        if (month < 1) {
            month = 12
            year--
        }
        loadMonthlyStatistics(year, month)
    }

    /**
     * 切换到下个月
     */
    fun nextMonth() {
        val state = currentState()
        var year = state.selectedYear
        var month = state.selectedMonth + 1
        if (month > 12) {
            month = 1
            year++
        }
        loadMonthlyStatistics(year, month)
    }

    /**
     * 切换年份
     */
    fun selectYear(year: Int) {
        if (currentState().isMonthlyView) {
            loadMonthlyStatistics(year, currentState().selectedMonth)
        } else {
            loadYearlyStatistics(year)
        }
    }
}

