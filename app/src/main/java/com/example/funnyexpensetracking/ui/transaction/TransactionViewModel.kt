package com.example.funnyexpensetracking.ui.transaction

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.sync.SyncManager
import com.example.funnyexpensetracking.data.sync.SyncState
import com.example.funnyexpensetracking.domain.model.Account
import com.example.funnyexpensetracking.domain.model.DailyTransactions
import com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency
import com.example.funnyexpensetracking.domain.model.FixedIncomeType
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.domain.usecase.RealtimeAssetCalculator
import com.example.funnyexpensetracking.domain.usecase.transaction.TransactionUseCases
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.DateTimeUtil
import com.example.funnyexpensetracking.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 交易记录ViewModel
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
    private val realtimeAssetCalculator: RealtimeAssetCalculator,
    private val userPreferencesManager: UserPreferencesManager
) : BaseViewModel<TransactionUiState, TransactionUiEvent>() {



    override fun initialState() = TransactionUiState()

    init {
        loadData()
        initDefaultAccounts()
        observeSyncState()
        observeRealtimeAsset()
    }

    /**
     * 观察实时资产变化
     */
    private fun observeRealtimeAsset() {
        realtimeAssetCalculator.realtimeAsset.onEach { assetData ->
            updateState {
                copy(
                    realtimeAsset = assetData.currentAsset,
                    incomePerMinute = assetData.incomePerMinute,
                    expensePerMinute = assetData.expensePerMinute,
                    netChangePerMinute = assetData.netChangePerMinute
                )
            }
        }.launchIn(viewModelScope)
    }

    /**
     * 观察同步状态
     */
    private fun observeSyncState() {
        syncManager.syncState.onEach { state ->
            updateState {
                copy(
                    isSyncing = state is SyncState.Syncing,
                    isOffline = !networkMonitor.isNetworkAvailable()
                )
            }
            when (state) {
                is SyncState.Success -> {
                    if (state.syncedCount > 0) {
                        sendEvent(TransactionUiEvent.ShowMessage("同步完成，共同步 ${state.syncedCount} 条记录"))
                    }
                }
                is SyncState.Error -> {
                    sendEvent(TransactionUiEvent.ShowMessage("同步失败: ${state.message}"))
                }
                else -> {}
            }
        }.launchIn(viewModelScope)

        // 观察待同步数量
        syncManager.pendingSyncCount.onEach { count ->
            updateState { copy(pendingSyncCount = count) }
        }.launchIn(viewModelScope)
    }

    /**
     * 手动触发同步
     */
    fun triggerSync() {
        viewModelScope.launch {
            if (!networkMonitor.isNetworkAvailable()) {
                sendEvent(TransactionUiEvent.ShowMessage("当前离线，数据将在网络恢复后自动同步"))
                return@launch
            }
            syncManager.syncAll()
        }
    }

    /**
     * 获取用户上次选择的账户ID
     */
    fun getLastSelectedAccountId(): Long {
        return userPreferencesManager.getLastSelectedAccountId()
    }

    /**
     * 保存用户选择的账户ID
     */
    fun saveLastSelectedAccountId(accountId: Long) {
        userPreferencesManager.saveLastSelectedAccountId(accountId)
    }

    /**
     * 初始化默认账户
     */
    private fun initDefaultAccounts() {
        viewModelScope.launch {
            transactionUseCases.initializeDefaultAccountsIfEmpty()
        }
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        // 获取今天的时间范围
        val todayStart = DateTimeUtil.getTodayStartTimestamp()
        val todayEnd = DateTimeUtil.getTodayEndTimestamp()

        transactionUseCases.loadTransactionsByDateRange(todayStart, todayEnd)
            .onEach { result ->
                updateState {
                    copy(
                        transactions = result.transactions,
                        dailyTransactions = result.dailyTransactions,
                        accounts = result.accounts,
                        todayIncome = result.todayIncome,
                        todayExpense = result.todayExpense,
                        totalBalance = result.totalBalance,
                        loadingState = LoadingState.SUCCESS
                    )
                }
            }.launchIn(viewModelScope)
    }



    /**
     * 显示添加交易对话框
     */
    fun showAddDialog() {
        updateState { copy(showAddDialog = true, editingTransaction = null) }
    }

    /**
     * 隐藏添加交易对话框
     */
    fun hideAddDialog() {
        updateState { copy(showAddDialog = false, editingTransaction = null) }
        sendEvent(TransactionUiEvent.DismissDialog)
    }

    /**
     * 显示添加账户对话框
     */
    fun showAddAccountDialog() {
        updateState { copy(showAddAccountDialog = true) }
    }

    /**
     * 隐藏添加账户对话框
     */
    fun hideAddAccountDialog() {
        updateState { copy(showAddAccountDialog = false) }
    }

    /**
     * 显示添加固定收支对话框
     */
    fun showAddFixedIncomeDialog() {
        updateState { copy(showAddFixedIncomeDialog = true) }
    }

    /**
     * 隐藏添加固定收支对话框
     */
    fun hideAddFixedIncomeDialog() {
        updateState { copy(showAddFixedIncomeDialog = false) }
    }

    /**
     * 添加交易记录
     * 离线优先：先保存本地，网络可用时自动同步
     */
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        accountId: Long,
        note: String = "",
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                transactionUseCases.addTransaction(
                    amount = amount,
                    type = type,
                    category = category,
                    accountId = accountId,
                    note = note,
                    date = date
                )

                hideAddDialog()
                sendEvent(TransactionUiEvent.TransactionAdded)

                // 根据网络状态显示不同提示
                val message = if (networkMonitor.isNetworkAvailable()) {
                    "记账成功"
                } else {
                    "记账成功（离线模式，稍后自动同步）"
                }
                sendEvent(TransactionUiEvent.ShowMessage(message))

                // 尝试同步
                if (networkMonitor.isNetworkAvailable()) {
                    syncManager.syncAll()
                }
            } catch (e: Exception) {
                updateState { copy(errorMessage = e.message) }
                sendEvent(TransactionUiEvent.ShowMessage("记账失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除交易记录
     * 离线优先：标记为待删除，网络可用时同步删除
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionUseCases.deleteTransaction(transaction)

                sendEvent(TransactionUiEvent.TransactionDeleted)
                sendEvent(TransactionUiEvent.ShowMessage("删除成功"))

                // 尝试同步
                if (networkMonitor.isNetworkAvailable()) {
                    syncManager.syncAll()
                }
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 编辑交易记录
     */
    fun editTransaction(transaction: Transaction) {
        updateState { copy(showAddDialog = true, editingTransaction = transaction) }
    }

    /**
     * 更新交易记录
     * 离线优先：先更新本地，标记待同步
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
                transactionUseCases.updateTransaction(
                    id = id,
                    amount = amount,
                    type = type,
                    category = category,
                    accountId = accountId,
                    note = note,
                    date = date
                )

                hideAddDialog()
                sendEvent(TransactionUiEvent.TransactionUpdated)
                sendEvent(TransactionUiEvent.ShowMessage("更新成功"))

                // 尝试同步
                if (networkMonitor.isNetworkAvailable()) {
                    syncManager.syncAll()
                }
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("更新失败: ${e.message}"))
            }
        }
    }

    /**
     * 添加账户
     */
    fun addAccount(name: String, initialBalance: Double = 0.0) {
        viewModelScope.launch {
            try {
                transactionUseCases.addAccount(
                    name = name,
                    initialBalance = initialBalance,
                    sortOrder = currentState().accounts.size
                )
                hideAddAccountDialog()
                sendEvent(TransactionUiEvent.AccountAdded)
                sendEvent(TransactionUiEvent.ShowMessage("账户添加成功"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("添加账户失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除账户
     */
    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                transactionUseCases.deleteAccount(account)
                sendEvent(TransactionUiEvent.ShowMessage("账户删除成功"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("删除账户失败: ${e.message}"))
            }
        }
    }

    /**
     * 更新账户（名称和余额）
     */
    fun updateAccount(accountId: Long, name: String, balance: Double) {
        viewModelScope.launch {
            try {
                transactionUseCases.updateAccount(accountId, name, balance)
                sendEvent(TransactionUiEvent.AccountUpdated)
                sendEvent(TransactionUiEvent.ShowMessage("账户更新成功"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("更新账户失败: ${e.message}"))
            }
        }
    }

    /**
     * 直接设置账户余额
     */
    fun setAccountBalance(accountId: Long, balance: Double) {
        viewModelScope.launch {
            try {
                transactionUseCases.setAccountBalance(accountId, balance)
                sendEvent(TransactionUiEvent.ShowMessage("余额已更新"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("更新余额失败: ${e.message}"))
            }
        }
    }

    /**
     * 添加固定收支
     * 固定收支会按分钟计算并实时更新资产
     */
    fun addFixedIncome(
        name: String,
        amount: Double,
        type: FixedIncomeType,
        frequency: FixedIncomeFrequency,
        startDate: Long,
        endDate: Long? = null
    ) {
        viewModelScope.launch {
            try {
                transactionUseCases.addFixedIncome(
                    name = name,
                    amount = amount,
                    type = type,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate
                )

                hideAddFixedIncomeDialog()
                sendEvent(TransactionUiEvent.FixedIncomeAdded)

                val typeText = if (type == FixedIncomeType.INCOME) "收入" else "支出"
                sendEvent(TransactionUiEvent.ShowMessage("固定$typeText 添加成功"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("添加固定收支失败: ${e.message}"))
            }
        }
    }


}

