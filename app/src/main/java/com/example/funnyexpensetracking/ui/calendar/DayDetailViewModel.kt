package com.example.funnyexpensetracking.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * 日期明细ViewModel
 */
@HiltViewModel
class DayDetailViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载指定日期的数据
     */
    fun loadDate(year: Int, month: Int, day: Int) {
        _uiState.value = _uiState.value.copy(
            year = year,
            month = month,
            day = day
        )

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTimestamp = calendar.timeInMillis
        val endTimestamp = startTimestamp + 24 * 60 * 60 * 1000 - 1

        transactionDao.getTransactionsByDateRange(startTimestamp, endTimestamp)
            .onEach { transactionEntities: List<TransactionEntity> ->
                viewModelScope.launch {
                    accountDao.getAllAccounts().collect { accountEntities ->
                        val accountMap = accountEntities.associateBy { it.id }

                        // 转换为领域模型
                        val transactions = transactionEntities.map { entity ->
                            entity.toDomainModel(accountMap[entity.accountId]?.name ?: "未知账户")
                        }.sortedByDescending { it.createdAt }

                        // 计算当天统计
                        val dayIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                        val dayExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                        _uiState.value = _uiState.value.copy(
                            transactions = transactions,
                            dayIncome = dayIncome,
                            dayExpense = dayExpense,
                            dayBalance = dayIncome - dayExpense
                        )
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
     * 删除交易记录
     */
    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionDao.deleteById(transactionId)
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
}

/**
 * 日期明细UI状态
 */
data class DayDetailUiState(
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val transactions: List<Transaction> = emptyList(),
    val dayIncome: Double = 0.0,
    val dayExpense: Double = 0.0,
    val dayBalance: Double = 0.0
)

