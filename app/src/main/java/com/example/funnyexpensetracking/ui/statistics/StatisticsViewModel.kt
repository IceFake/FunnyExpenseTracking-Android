package com.example.funnyexpensetracking.ui.statistics

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.usecase.statistics.*
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
    private val getMonthlyStatisticsUseCase: GetMonthlyStatisticsUseCase,
    private val getYearlyStatisticsUseCase: GetYearlyStatisticsUseCase,
    private val getCategoryStatisticsUseCase: GetCategoryStatisticsUseCase,
    private val getTrendStatisticsUseCase: GetTrendStatisticsUseCase
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
            when (val result = getMonthlyStatisticsUseCase(year, month)) {
                is Resource.Success -> {
                    updateState {
                        copy(
                            currentStatistics = result.data,
                            chartUrl = result.data?.chartUrl,
                            loadingState = LoadingState.SUCCESS
                        )
                    }
                    // 同时加载分类统计
                    loadCategoryStatistics(year, month)
                }
                is Resource.Error -> {
                    updateState {
                        copy(
                            loadingState = LoadingState.ERROR,
                            errorMessage = result.message
                        )
                    }
                    sendEvent(StatisticsUiEvent.ShowMessage(result.message ?: "加载失败"))
                }
                is Resource.Loading -> {
                    // 保持加载状态
                }
            }
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
            when (val result = getYearlyStatisticsUseCase(year)) {
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
                    sendEvent(StatisticsUiEvent.ShowMessage(result.message ?: "加载失败"))
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
            when (val result = getCategoryStatisticsUseCase(year, month)) {
                is Resource.Success -> {
                    updateState { copy(categoryStats = result.data ?: emptyList()) }
                }
                is Resource.Error -> {
                    // 分类统计加载失败不影响主页面
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 加载趋势统计
     */
    fun loadTrendStatistics(months: Int = 6) {
        viewModelScope.launch {
            when (val result = getTrendStatisticsUseCase(months)) {
                is Resource.Success -> {
                    updateState { copy(trendStatistics = result.data) }
                }
                is Resource.Error -> {
                    sendEvent(StatisticsUiEvent.ShowMessage(result.message ?: "加载趋势失败"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 切换到月视图
     */
    fun switchToMonthlyView() {
        if (!currentState().isMonthlyView) {
            loadMonthlyStatistics(currentState().selectedYear, currentState().selectedMonth)
        }
    }

    /**
     * 切换到年视图
     */
    fun switchToYearlyView() {
        if (currentState().isMonthlyView) {
            loadYearlyStatistics(currentState().selectedYear)
        }
    }

    /**
     * 选择年月
     */
    fun selectYearMonth(year: Int, month: Int) {
        if (currentState().isMonthlyView) {
            loadMonthlyStatistics(year, month)
        } else {
            loadYearlyStatistics(year)
        }
    }

    /**
     * 打开图表
     */
    fun openChart() {
        currentState().chartUrl?.let { url ->
            sendEvent(StatisticsUiEvent.OpenChart(url))
        }
    }
}

