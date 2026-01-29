package com.example.funnyexpensetracking.ui.transaction

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.domain.usecase.transaction.*
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 交易记录ViewModel
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase
) : BaseViewModel<TransactionUiState, TransactionUiEvent>() {

    override fun initialState() = TransactionUiState()

    init {
        loadTodayTransactions()
    }

    /**
     * 加载今天的交易记录
     */
    fun loadTodayTransactions() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        val startOfDay = DateTimeUtil.getTodayStartTimestamp()
        val endOfDay = DateTimeUtil.getTodayEndTimestamp()

        getTransactionsUseCase.byDateRange(startOfDay, endOfDay)
            .onEach { transactions ->
                val todayIncome = transactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val todayExpense = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                updateState {
                    copy(
                        transactions = transactions,
                        loadingState = LoadingState.SUCCESS,
                        todayIncome = todayIncome,
                        todayExpense = todayExpense
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 加载指定日期范围的交易记录
     */
    fun loadTransactionsByDateRange(startDate: Long, endDate: Long) {
        updateState { copy(loadingState = LoadingState.LOADING) }

        getTransactionsUseCase.byDateRange(startDate, endDate)
            .onEach { transactions ->
                updateState {
                    copy(
                        transactions = transactions,
                        loadingState = LoadingState.SUCCESS
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 添加交易记录
     */
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        note: String = "",
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    note = note,
                    date = date
                )
                addTransactionUseCase(transaction)
                sendEvent(TransactionUiEvent.TransactionAdded)
                sendEvent(TransactionUiEvent.ShowMessage("记账成功"))
            } catch (e: Exception) {
                updateState { copy(errorMessage = e.message) }
                sendEvent(TransactionUiEvent.ShowMessage("记账失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除交易记录
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                deleteTransactionUseCase(transaction)
                sendEvent(TransactionUiEvent.TransactionDeleted)
                sendEvent(TransactionUiEvent.ShowMessage("删除成功"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 更新交易记录
     */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                updateTransactionUseCase(transaction)
                sendEvent(TransactionUiEvent.ShowMessage("更新成功"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("更新失败: ${e.message}"))
            }
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(date: Long) {
        updateState { copy(selectedDate = date) }
        val startOfDay = DateTimeUtil.getTodayStartTimestamp()
        val endOfDay = DateTimeUtil.getTodayEndTimestamp()
        loadTransactionsByDateRange(startOfDay, endOfDay)
    }
}

