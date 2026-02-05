package com.example.funnyexpensetracking.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.domain.model.Account
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _uiEvent = MutableSharedFlow<DayDetailUiEvent>()
    val uiEvent: SharedFlow<DayDetailUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountDao.getAllAccounts().collect { accountEntities ->
                _accounts.value = accountEntities.map { entity ->
                    Account(
                        id = entity.id,
                        name = entity.name,
                        icon = entity.icon,
                        balance = entity.balance,
                        isDefault = entity.isDefault,
                        sortOrder = entity.sortOrder
                    )
                }
            }
        }
    }

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
                // 获取要删除的交易记录
                val transaction = transactionDao.getById(transactionId)
                if (transaction != null) {
                    // 回滚账户余额
                    val balanceChange = if (transaction.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME) {
                        -transaction.amount
                    } else {
                        transaction.amount
                    }
                    accountDao.updateBalance(transaction.accountId, balanceChange)
                }
                transactionDao.deleteById(transactionId)
                _uiEvent.emit(DayDetailUiEvent.ShowMessage("删除成功"))
            } catch (e: Exception) {
                _uiEvent.emit(DayDetailUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 更新交易记录
     */
    fun updateTransaction(
        id: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        accountId: Long,
        note: String,
        date: Long
    ) {
        viewModelScope.launch {
            try {
                val existingEntity = transactionDao.getById(id) ?: return@launch

                // 回滚旧账户余额
                val oldBalanceChange = if (existingEntity.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME) {
                    -existingEntity.amount
                } else {
                    existingEntity.amount
                }
                accountDao.updateBalance(existingEntity.accountId, oldBalanceChange)

                // 更新交易记录
                val entityType = when (type) {
                    TransactionType.INCOME -> com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME
                    TransactionType.EXPENSE -> com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE
                }
                val entity = TransactionEntity(
                    id = id,
                    serverId = existingEntity.serverId,
                    amount = amount,
                    type = entityType,
                    category = category,
                    accountId = accountId,
                    note = note,
                    date = date,
                    createdAt = existingEntity.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                transactionDao.update(entity)

                // 更新新账户余额
                val newBalanceChange = if (type == TransactionType.INCOME) amount else -amount
                accountDao.updateBalance(accountId, newBalanceChange)

                _uiEvent.emit(DayDetailUiEvent.ShowMessage("更新成功"))
            } catch (e: Exception) {
                _uiEvent.emit(DayDetailUiEvent.ShowMessage("更新失败: ${e.message}"))
            }
        }
    }
}

/**
 * 日期明细UI事件
 */
sealed class DayDetailUiEvent {
    data class ShowMessage(val message: String) : DayDetailUiEvent()
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

