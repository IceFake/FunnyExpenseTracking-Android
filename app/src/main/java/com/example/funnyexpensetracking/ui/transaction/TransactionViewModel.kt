package com.example.funnyexpensetracking.ui.transaction

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.AccountEntity
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.local.entity.TransactionType as EntityTransactionType
import com.example.funnyexpensetracking.data.sync.SyncManager
import com.example.funnyexpensetracking.data.sync.SyncState
import com.example.funnyexpensetracking.domain.model.Account
import com.example.funnyexpensetracking.domain.model.DailyTransactions
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
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
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor
) : BaseViewModel<TransactionUiState, TransactionUiEvent>() {

    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.CHINA)

    override fun initialState() = TransactionUiState()

    init {
        loadData()
        initDefaultAccounts()
        observeSyncState()
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
     * 初始化默认账户
     */
    private fun initDefaultAccounts() {
        viewModelScope.launch {
            val accounts = accountDao.getAllAccounts()
            accounts.collect { list ->
                if (list.isEmpty()) {
                    // 创建默认账户
                    val defaultAccounts = listOf(
                        AccountEntity(name = "现金", icon = "cash", balance = 0.0, isDefault = true, sortOrder = 0),
                        AccountEntity(name = "微信", icon = "wechat", balance = 0.0, isDefault = false, sortOrder = 1),
                        AccountEntity(name = "支付宝", icon = "alipay", balance = 0.0, isDefault = false, sortOrder = 2),
                        AccountEntity(name = "银行卡", icon = "bank", balance = 0.0, isDefault = false, sortOrder = 3)
                    )
                    defaultAccounts.forEach { accountDao.insert(it) }
                }
            }
        }
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        // 组合账户和交易数据
        combine(
            transactionDao.getAllTransactions(),
            accountDao.getAllAccounts()
        ) { transactions, accounts ->
            Pair(transactions, accounts)
        }.onEach { (transactionEntities, accountEntities) ->
            val accountMap = accountEntities.associateBy { it.id }
            val accountList = accountEntities.map { it.toDomainModel() }

            // 转换为领域模型
            val transactions = transactionEntities.map { entity ->
                entity.toDomainModel(accountMap[entity.accountId]?.name ?: "未知账户")
            }

            // 按日期分组
            val dailyTransactions = groupTransactionsByDate(transactions)

            // 计算今日收支
            val today = DateTimeUtil.getTodayStartTimestamp()
            val todayEnd = DateTimeUtil.getTodayEndTimestamp()
            val todayTransactions = transactions.filter { it.date in today..todayEnd }
            val todayIncome = todayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val todayExpense = todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            // 计算总余额
            val totalBalance = accountEntities.sumOf { it.balance }

            updateState {
                copy(
                    transactions = transactions,
                    dailyTransactions = dailyTransactions,
                    accounts = accountList,
                    todayIncome = todayIncome,
                    todayExpense = todayExpense,
                    totalBalance = totalBalance,
                    loadingState = LoadingState.SUCCESS
                )
            }
        }.launchIn(viewModelScope)
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
                val entity = TransactionEntity(
                    amount = amount,
                    type = type.toEntityType(),
                    category = category,
                    accountId = accountId,
                    note = note,
                    date = date,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    updatedAt = System.currentTimeMillis()
                )
                transactionDao.insert(entity)

                // 更新账户余额
                val balanceChange = if (type == TransactionType.INCOME) amount else -amount
                accountDao.updateBalance(accountId, balanceChange)

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
                val entity = transactionDao.getById(transaction.id)

                if (entity != null) {
                    if (entity.serverId != null) {
                        // 有服务器ID，标记为待删除（软删除）
                        transactionDao.markAsDeleted(transaction.id)
                    } else {
                        // 没有服务器ID，直接删除本地记录
                        transactionDao.delete(entity)
                    }

                    // 回滚账户余额
                    val balanceChange = if (transaction.type == TransactionType.INCOME) -transaction.amount else transaction.amount
                    accountDao.updateBalance(transaction.accountId, balanceChange)

                    sendEvent(TransactionUiEvent.TransactionDeleted)
                    sendEvent(TransactionUiEvent.ShowMessage("删除成功"))

                    // 尝试同步
                    if (networkMonitor.isNetworkAvailable()) {
                        syncManager.syncAll()
                    }
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
                val oldTransaction = currentState().editingTransaction ?: return@launch
                val existingEntity = transactionDao.getById(id)

                // 回滚旧账户余额
                val oldBalanceChange = if (oldTransaction.type == TransactionType.INCOME) -oldTransaction.amount else oldTransaction.amount
                accountDao.updateBalance(oldTransaction.accountId, oldBalanceChange)

                // 更新交易记录
                val entity = TransactionEntity(
                    id = id,
                    serverId = existingEntity?.serverId,
                    amount = amount,
                    type = type.toEntityType(),
                    category = category,
                    accountId = accountId,
                    note = note,
                    date = date,
                    createdAt = existingEntity?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                transactionDao.update(entity)

                // 更新新账户余额
                val newBalanceChange = if (type == TransactionType.INCOME) amount else -amount
                accountDao.updateBalance(accountId, newBalanceChange)

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
                val entity = AccountEntity(
                    name = name,
                    balance = initialBalance,
                    sortOrder = currentState().accounts.size
                )
                accountDao.insert(entity)
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
                val entity = AccountEntity(
                    id = account.id,
                    name = account.name,
                    icon = account.icon,
                    balance = account.balance,
                    isDefault = account.isDefault,
                    sortOrder = account.sortOrder
                )
                accountDao.delete(entity)
                sendEvent(TransactionUiEvent.ShowMessage("账户删除成功"))
            } catch (e: Exception) {
                sendEvent(TransactionUiEvent.ShowMessage("删除账户失败: ${e.message}"))
            }
        }
    }

    // ========== 类型转换扩展函数 ==========

    private fun TransactionEntity.toDomainModel(accountName: String): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = type.toDomainType(),
            category = category,
            accountId = accountId,
            accountName = accountName,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    private fun AccountEntity.toDomainModel(): Account {
        return Account(
            id = id,
            name = name,
            icon = icon,
            balance = balance,
            isDefault = isDefault,
            sortOrder = sortOrder
        )
    }

    private fun TransactionType.toEntityType(): EntityTransactionType {
        return when (this) {
            TransactionType.INCOME -> EntityTransactionType.INCOME
            TransactionType.EXPENSE -> EntityTransactionType.EXPENSE
        }
    }

    private fun EntityTransactionType.toDomainType(): TransactionType {
        return when (this) {
            EntityTransactionType.INCOME -> TransactionType.INCOME
            EntityTransactionType.EXPENSE -> TransactionType.EXPENSE
        }
    }
}

