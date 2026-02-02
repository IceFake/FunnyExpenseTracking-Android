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

    override fun initialState(): HistoryUiState {
        val today = DateTimeUtil.getTodayStartTimestamp()
        return HistoryUiState(
            selectedDate = today
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

        val selectedDate = currentState().selectedDate
        val startTimestamp = selectedDate // 当天0点
        val endTimestamp = selectedDate + 24 * 60 * 60 * 1000 - 1 // 当天23:59:59

        transactionDao.getTransactionsByDateRange(startTimestamp, endTimestamp)
            .onEach { transactionEntities: List<TransactionEntity> ->
                viewModelScope.launch {
                    val accountsFlow = accountDao.getAllAccounts()
                    accountsFlow.collect { accountEntities ->
                        val accountMap = accountEntities.associateBy { it.id }

                        // 转换为领域模型
                        val transactions = transactionEntities.map { entity ->
                            entity.toDomainModel(accountMap[entity.accountId]?.name ?: "未知账户")
                        }.sortedByDescending { it.createdAt }

                        // 计算当天统计
                        val dayIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                        val dayExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                        updateState {
                            copy(
                                transactions = transactions,
                                dayIncome = dayIncome,
                                dayExpense = dayExpense,
                                dayBalance = dayIncome - dayExpense,
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
     * 选择上一天
     */
    fun selectPreviousDay() {
        val currentDate = currentState().selectedDate
        val previousDay = currentDate - 24 * 60 * 60 * 1000

        updateState {
            copy(selectedDate = previousDay)
        }
        loadData()
    }

    /**
     * 选择下一天
     */
    fun selectNextDay() {
        val currentDate = currentState().selectedDate
        val nextDay = currentDate + 24 * 60 * 60 * 1000

        updateState {
            copy(selectedDate = nextDay)
        }
        loadData()
    }

    /**
     * 选择指定日期
     */
    fun selectDate(year: Int, month: Int, day: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        updateState {
            copy(selectedDate = calendar.timeInMillis)
        }
        loadData()
    }

    /**
     * 获取显示的日期字符串
     */
    fun getDisplayDate(): String {
        val date = Date(currentState().selectedDate)
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA)
        return dateFormat.format(date)
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

