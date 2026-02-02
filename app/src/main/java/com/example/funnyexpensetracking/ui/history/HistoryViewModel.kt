package com.example.funnyexpensetracking.ui.history

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.domain.model.DailyTransactions
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 历史账单ViewModel
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : BaseViewModel<HistoryUiState, HistoryUiEvent>() {

    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.CHINA)

    override fun initialState(): HistoryUiState {
        val year = DateTimeUtil.getCurrentYear()
        val month = DateTimeUtil.getCurrentMonth()
        return HistoryUiState(
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
                viewModelScope.launch {
                    val accountsFlow = accountDao.getAllAccounts()
                    accountsFlow.collect { accountEntities ->
                        val accountMap = accountEntities.associateBy { it.id }

                        // 转换为领域模型
                        val transactions = transactionEntities.map { entity ->
                            entity.toDomainModel(accountMap[entity.accountId]?.name ?: "未知账户")
                        }

                        // 按日期分组
                        val dailyTransactions = groupTransactionsByDate(transactions)

                        // 计算月度统计
                        val monthIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                        val monthExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                        updateState {
                            copy(
                                dailyTransactions = dailyTransactions,
                                monthIncome = monthIncome,
                                monthExpense = monthExpense,
                                monthBalance = monthIncome - monthExpense,
                                loadingState = LoadingState.SUCCESS
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    /**
     * 将 TransactionEntity 转换为领域模型
     */
    private fun TransactionEntity.toDomainModel(accountName: String): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = when (type) {
                com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME -> TransactionType.INCOME
                com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE -> TransactionType.EXPENSE
            },
            category = category,
            accountId = accountId,
            accountName = accountName,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    /**
     * 按日期分组交易记录
     */
    private fun groupTransactionsByDate(transactions: List<Transaction>): List<DailyTransactions> {
        return transactions
            .groupBy { getDateOnly(it.date) }
            .map { (dateTimestamp, transactionList) ->
                DailyTransactions(
                    date = dateTimestamp,
                    dateString = dateFormat.format(Date(dateTimestamp)),
                    dayOfWeek = dayOfWeekFormat.format(Date(dateTimestamp)),
                    totalIncome = transactionList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    totalExpense = transactionList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    transactions = transactionList.sortedByDescending { it.createdAt }
                )
            }
            .sortedByDescending { it.date }
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
        return "${year}年${String.format("%02d", month)}月"
    }

    /**
     * 删除交易记录
     */
    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionDao.deleteById(transactionId)
                sendEvent(HistoryUiEvent.TransactionDeleted)
            } catch (e: Exception) {
                sendEvent(HistoryUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }
}

