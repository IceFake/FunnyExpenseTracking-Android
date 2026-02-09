package com.example.funnyexpensetracking.ui.investment

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.InvestmentDao
import com.example.funnyexpensetracking.data.local.entity.InvestmentEntity
import com.example.funnyexpensetracking.data.local.entity.InvestmentCategory as EntityCategory
import com.example.funnyexpensetracking.domain.model.Investment
import com.example.funnyexpensetracking.domain.model.InvestmentCategory
import com.example.funnyexpensetracking.domain.repository.InvestmentRepository
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 投资ViewModel
 */
@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val investmentDao: InvestmentDao,
    private val investmentRepository: InvestmentRepository
) : BaseViewModel<InvestmentUiState, InvestmentUiEvent>() {

    private var stockPriceRefreshJob: Job? = null

    override fun initialState() = InvestmentUiState()

    init {
        loadInvestments()
        startStockPriceRefresh()
    }

    /**
     * 加载投资数据
     */
    private fun loadInvestments() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        investmentDao.getAllInvestments()
            .onEach { entities ->
                val investments = entities.map { it.toDomainModel() }

                // 合并相同描述的条目用于显示
                val mergedInvestments = mergeInvestments(investments)

                // 计算总值
                val totalInvestment = mergedInvestments.sumOf { it.investment }
                val totalCurrentValue = mergedInvestments.sumOf { it.calcCurrentValue() }
                val totalProfitLoss = totalCurrentValue - totalInvestment

                updateState {
                    copy(
                        allInvestments = mergedInvestments,
                        filteredInvestments = applyFilter(mergedInvestments, filterType),
                        totalInvestment = totalInvestment,
                        totalCurrentValue = totalCurrentValue,
                        totalProfitLoss = totalProfitLoss,
                        loadingState = LoadingState.SUCCESS
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 合并相同描述/代码的投资条目
     */
    private fun mergeInvestments(investments: List<Investment>): List<Investment> {
        return investments
            .groupBy { "${it.category}_${it.description}" }
            .map { (_, items) ->
                if (items.size == 1) {
                    items.first()
                } else {
                    val first = items.first()
                    first.copy(
                        quantity = items.sumOf { it.quantity },
                        investment = items.sumOf { it.investment },
                        currentValue = items.sumOf { it.currentValue }
                    )
                }
            }
    }

    /**
     * 设置筛选类型
     */
    fun setFilterType(filterType: InvestmentFilterType) {
        updateState {
            copy(
                filterType = filterType,
                filteredInvestments = applyFilter(allInvestments, filterType)
            )
        }
    }

    /**
     * 应用筛选
     */
    private fun applyFilter(list: List<Investment>, filterType: InvestmentFilterType): List<Investment> {
        return when (filterType) {
            InvestmentFilterType.ALL -> list
            InvestmentFilterType.STOCK -> list.filter { it.category == InvestmentCategory.STOCK }
            InvestmentFilterType.OTHER -> list.filter { it.category == InvestmentCategory.OTHER }
        }
    }

    /**
     * 显示添加弹窗
     */
    fun showAddDialog() {
        updateState { copy(showAddDialog = true, editingInvestment = null) }
    }

    /**
     * 隐藏添加弹窗
     */
    fun hideAddDialog() {
        updateState { copy(showAddDialog = false, editingInvestment = null) }
        sendEvent(InvestmentUiEvent.DismissDialog)
    }

    /**
     * 编辑投资条目
     */
    fun editInvestment(investment: Investment) {
        updateState { copy(showAddDialog = true, editingInvestment = investment) }
    }

    /**
     * 添加投资条目
     */
    fun addInvestment(
        category: InvestmentCategory,
        description: String,
        quantity: Double,
        investment: Double,
        currentValue: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val entityCategory = when (category) {
                    InvestmentCategory.STOCK -> EntityCategory.STOCK
                    InvestmentCategory.OTHER -> EntityCategory.OTHER
                }

                // 检查是否存在相同描述的条目
                val existing = investmentDao.findByDescriptionAndCategory(description, entityCategory)

                if (existing != null) {
                    // 累加到现有条目
                    val updated = existing.copy(
                        quantity = existing.quantity + quantity,
                        investment = existing.investment + investment,
                        // 其他类型：如果提供了当前价值则更新，否则累加
                        currentValue = if (category == InvestmentCategory.OTHER && currentValue != null) {
                            existing.currentValue + currentValue
                        } else {
                            existing.currentValue
                        },
                        updatedAt = System.currentTimeMillis()
                    )
                    investmentDao.update(updated)
                    sendEvent(InvestmentUiEvent.ShowMessage("已合并到现有条目"))
                } else {
                    // 创建新条目
                    val entity = InvestmentEntity(
                        category = entityCategory,
                        description = description,
                        quantity = quantity,
                        investment = investment,
                        currentPrice = 0.0, // 股票用，暂时不接API
                        currentValue = currentValue ?: investment // 其他类型：默认等于投入
                    )
                    investmentDao.insert(entity)
                    sendEvent(InvestmentUiEvent.ShowMessage("添加成功"))
                }

                hideAddDialog()
                sendEvent(InvestmentUiEvent.InvestmentAdded)
            } catch (e: Exception) {
                sendEvent(InvestmentUiEvent.ShowMessage("添加失败: ${e.message}"))
            }
        }
    }

    /**
     * 更新投资条目
     */
    fun updateInvestment(
        id: Long,
        category: InvestmentCategory,
        description: String,
        quantity: Double,
        investment: Double,
        currentValue: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val existing = investmentDao.getById(id) ?: return@launch

                val entityCategory = when (category) {
                    InvestmentCategory.STOCK -> EntityCategory.STOCK
                    InvestmentCategory.OTHER -> EntityCategory.OTHER
                }

                val updated = existing.copy(
                    category = entityCategory,
                    description = description,
                    quantity = quantity,
                    investment = investment,
                    // 股票不允许手动编辑当前价值
                    currentPrice = if (category == InvestmentCategory.STOCK) existing.currentPrice else 0.0,
                    // 其他类型可以手动编辑当前价值
                    currentValue = if (category == InvestmentCategory.OTHER) {
                        currentValue ?: investment
                    } else {
                        existing.currentValue
                    },
                    updatedAt = System.currentTimeMillis()
                )

                investmentDao.update(updated)
                hideAddDialog()
                sendEvent(InvestmentUiEvent.ShowMessage("更新成功"))
                sendEvent(InvestmentUiEvent.InvestmentUpdated)
            } catch (e: Exception) {
                sendEvent(InvestmentUiEvent.ShowMessage("更新失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除投资条目
     */
    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            try {
                investmentDao.deleteById(investment.id)
                sendEvent(InvestmentUiEvent.ShowMessage("删除成功"))
                sendEvent(InvestmentUiEvent.InvestmentDeleted)
            } catch (e: Exception) {
                sendEvent(InvestmentUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * Entity转Domain Model
     */
    private fun InvestmentEntity.toDomainModel(): Investment {
        return Investment(
            id = id,
            category = when (category) {
                EntityCategory.STOCK -> InvestmentCategory.STOCK
                EntityCategory.OTHER -> InvestmentCategory.OTHER
            },
            description = description,
            quantity = quantity,
            investment = investment,
            currentPrice = currentPrice,
            currentValue = currentValue,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 每分钟自动刷新股票价格
     */
    private fun startStockPriceRefresh() {
        stockPriceRefreshJob?.cancel()
        stockPriceRefreshJob = viewModelScope.launch {
            // 初始刷新一次
            refreshStockPricesInternal()

            // 每分钟刷新
            while (isActive) {
                delay(60_000) // 60秒
                refreshStockPricesInternal()
            }
        }
    }

    /**
     * 内部刷新股票价格
     */
    private suspend fun refreshStockPricesInternal() {
        when (val result = investmentRepository.refreshAllStockPrices()) {
            is Resource.Success -> {
                // 刷新成功，UI会通过Flow自动更新
            }
            is Resource.Error -> {
                // 静默失败，不显示错误消息
            }
            is Resource.Loading -> {}
        }
    }

    /**
     * 手动刷新股票价格
     */
    fun refreshStockPrices() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            when (val result = investmentRepository.refreshAllStockPrices()) {
                is Resource.Success -> {
                    sendEvent(InvestmentUiEvent.ShowMessage("刷新成功"))
                }
                is Resource.Error -> {
                    sendEvent(InvestmentUiEvent.ShowMessage("刷新失败: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
            updateState { copy(isRefreshing = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stockPriceRefreshJob?.cancel()
    }
}

