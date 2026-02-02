package com.example.funnyexpensetracking.ui.calendar

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.local.entity.TransactionType
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import javax.inject.Inject

/**
 * 日历ViewModel
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val transactionDao: TransactionDao
) : BaseViewModel<CalendarUiState, CalendarUiEvent>() {

    override fun initialState(): CalendarUiState {
        val year = DateTimeUtil.getCurrentYear()
        val month = DateTimeUtil.getCurrentMonth()
        return CalendarUiState(
            selectedYear = year,
            selectedMonth = month
        )
    }

    init {
        loadData()
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        val year = currentState().selectedYear
        val month = currentState().selectedMonth
        val startTimestamp = DateTimeUtil.getMonthStartTimestamp(year, month)
        val endTimestamp = DateTimeUtil.getMonthEndTimestamp(year, month)

        transactionDao.getTransactionsByDateRange(startTimestamp, endTimestamp)
            .onEach { transactionEntities: List<TransactionEntity> ->
                // 计算每日收支差额
                val dailyBalanceMap = calculateDailyBalance(transactionEntities)

                // 计算月度统计
                val monthIncome = transactionEntities
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val monthExpense = transactionEntities
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                updateState {
                    copy(
                        dailyBalanceMap = dailyBalanceMap,
                        monthIncome = monthIncome,
                        monthExpense = monthExpense,
                        monthBalance = monthIncome - monthExpense,
                        loadingState = LoadingState.SUCCESS
                    )
                }
            }.launchIn(viewModelScope)
    }

    /**
     * 计算每日收支差额
     */
    private fun calculateDailyBalance(transactions: List<TransactionEntity>): Map<Long, Double> {
        return transactions
            .groupBy { getDateOnly(it.date) }
            .mapValues { (_, transactionList) ->
                val income = transactionList
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expense = transactionList
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                income - expense
            }
    }

    /**
     * 获取日期的0点时间戳
     */
    private fun getDateOnly(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 选择上一个月
     */
    fun selectPreviousMonth() {
        val currentYear = currentState().selectedYear
        val currentMonth = currentState().selectedMonth

        val newYear: Int
        val newMonth: Int
        if (currentMonth == 1) {
            newYear = currentYear - 1
            newMonth = 12
        } else {
            newYear = currentYear
            newMonth = currentMonth - 1
        }

        updateState {
            copy(selectedYear = newYear, selectedMonth = newMonth)
        }
        loadData()
    }

    /**
     * 选择下一个月
     */
    fun selectNextMonth() {
        val currentYear = currentState().selectedYear
        val currentMonth = currentState().selectedMonth

        val newYear: Int
        val newMonth: Int
        if (currentMonth == 12) {
            newYear = currentYear + 1
            newMonth = 1
        } else {
            newYear = currentYear
            newMonth = currentMonth + 1
        }

        updateState {
            copy(selectedYear = newYear, selectedMonth = newMonth)
        }
        loadData()
    }

    /**
     * 选择指定月份
     */
    fun selectMonth(year: Int, month: Int) {
        updateState {
            copy(selectedYear = year, selectedMonth = month)
        }
        loadData()
    }

    /**
     * 获取显示的月份字符串
     */
    fun getDisplayMonth(): String {
        val year = currentState().selectedYear
        val month = currentState().selectedMonth
        return "${year}年${String.format(java.util.Locale.CHINA, "%02d", month)}月"
    }

    /**
     * 点击日期
     */
    fun onDateClick(year: Int, month: Int, day: Int) {
        sendEvent(CalendarUiEvent.NavigateToDay(year, month, day))
    }
}

