package com.example.funnyexpensetracking.ui.fixedincome

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeEntity
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency as EntityFrequency
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType as EntityType
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency
import com.example.funnyexpensetracking.domain.model.FixedIncomeType
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 固定收支ViewModel
 */
@HiltViewModel
class FixedIncomeViewModel @Inject constructor(
    private val fixedIncomeDao: FixedIncomeDao
) : BaseViewModel<FixedIncomeUiState, FixedIncomeUiEvent>() {

    override fun initialState() = FixedIncomeUiState()

    init {
        loadFixedIncomes()
    }

    /**
     * 加载固定收支数据
     */
    private fun loadFixedIncomes() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        fixedIncomeDao.getAllFixedIncomes()
            .onEach { entities ->
                val fixedIncomes = entities.map { it.toDomainModel() }

                // 计算每分钟收入/支出
                val incomePerMinute = fixedIncomes
                    .filter { it.type == FixedIncomeType.INCOME && it.isActive }
                    .sumOf { it.getAmountPerMinute() }

                val expensePerMinute = fixedIncomes
                    .filter { it.type == FixedIncomeType.EXPENSE && it.isActive }
                    .sumOf { it.getAmountPerMinute() }

                val netPerMinute = incomePerMinute - expensePerMinute

                updateState {
                    copy(
                        allFixedIncomes = fixedIncomes,
                        filteredFixedIncomes = applyFilter(fixedIncomes, filterType),
                        incomePerMinute = incomePerMinute,
                        expensePerMinute = expensePerMinute,
                        netPerMinute = netPerMinute,
                        loadingState = LoadingState.SUCCESS
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 设置筛选类型
     */
    fun setFilterType(filterType: FixedIncomeFilterType) {
        updateState {
            copy(
                filterType = filterType,
                filteredFixedIncomes = applyFilter(allFixedIncomes, filterType)
            )
        }
    }

    /**
     * 应用筛选
     */
    private fun applyFilter(list: List<FixedIncome>, filterType: FixedIncomeFilterType): List<FixedIncome> {
        return when (filterType) {
            FixedIncomeFilterType.ALL -> list
            FixedIncomeFilterType.INCOME -> list.filter { it.type == FixedIncomeType.INCOME }
            FixedIncomeFilterType.EXPENSE -> list.filter { it.type == FixedIncomeType.EXPENSE }
        }
    }

    /**
     * 显示添加弹窗
     */
    fun showAddDialog() {
        updateState { copy(showAddDialog = true, editingFixedIncome = null) }
    }

    /**
     * 隐藏添加弹窗
     */
    fun hideAddDialog() {
        updateState { copy(showAddDialog = false, editingFixedIncome = null) }
        sendEvent(FixedIncomeUiEvent.DismissDialog)
    }

    /**
     * 编辑固定收支
     */
    fun editFixedIncome(fixedIncome: FixedIncome) {
        updateState { copy(showAddDialog = true, editingFixedIncome = fixedIncome) }
    }

    /**
     * 添加固定收支
     */
    fun addFixedIncome(
        name: String,
        amount: Double,
        type: FixedIncomeType,
        frequency: FixedIncomeFrequency,
        startDate: Long
    ) {
        viewModelScope.launch {
            try {
                val entity = FixedIncomeEntity(
                    name = name,
                    amount = amount,
                    type = type.toEntityType(),
                    frequency = frequency.toEntityFrequency(),
                    startDate = startDate,
                    isActive = true
                )
                fixedIncomeDao.insert(entity)
                hideAddDialog()
                sendEvent(FixedIncomeUiEvent.FixedIncomeAdded)

                val typeText = if (type == FixedIncomeType.INCOME) "收入" else "支出"
                sendEvent(FixedIncomeUiEvent.ShowMessage("固定$typeText 添加成功"))
            } catch (e: Exception) {
                sendEvent(FixedIncomeUiEvent.ShowMessage("添加失败: ${e.message}"))
            }
        }
    }

    /**
     * 更新固定收支
     */
    fun updateFixedIncome(
        id: Long,
        name: String,
        amount: Double,
        type: FixedIncomeType,
        frequency: FixedIncomeFrequency,
        startDate: Long
    ) {
        viewModelScope.launch {
            try {
                val existingEntity = fixedIncomeDao.getById(id)
                val entity = FixedIncomeEntity(
                    id = id,
                    name = name,
                    amount = amount,
                    type = type.toEntityType(),
                    frequency = frequency.toEntityFrequency(),
                    startDate = startDate,
                    isActive = existingEntity?.isActive ?: true,
                    createdAt = existingEntity?.createdAt ?: System.currentTimeMillis()
                )
                fixedIncomeDao.update(entity)
                hideAddDialog()
                sendEvent(FixedIncomeUiEvent.FixedIncomeUpdated)
                sendEvent(FixedIncomeUiEvent.ShowMessage("更新成功"))
            } catch (e: Exception) {
                sendEvent(FixedIncomeUiEvent.ShowMessage("更新失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除固定收支
     */
    fun deleteFixedIncome(fixedIncome: FixedIncome) {
        viewModelScope.launch {
            try {
                val entity = FixedIncomeEntity(
                    id = fixedIncome.id,
                    name = fixedIncome.name,
                    amount = fixedIncome.amount,
                    type = fixedIncome.type.toEntityType(),
                    frequency = fixedIncome.frequency.toEntityFrequency(),
                    startDate = fixedIncome.startDate,
                    endDate = fixedIncome.endDate,
                    isActive = fixedIncome.isActive
                )
                fixedIncomeDao.delete(entity)
                sendEvent(FixedIncomeUiEvent.FixedIncomeDeleted)
                sendEvent(FixedIncomeUiEvent.ShowMessage("删除成功"))
            } catch (e: Exception) {
                sendEvent(FixedIncomeUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 切换固定收支启用状态
     */
    fun toggleFixedIncomeStatus(fixedIncome: FixedIncome) {
        viewModelScope.launch {
            try {
                fixedIncomeDao.updateActiveStatus(fixedIncome.id, !fixedIncome.isActive)
                val statusText = if (fixedIncome.isActive) "已停用" else "已启用"
                sendEvent(FixedIncomeUiEvent.ShowMessage(statusText))
            } catch (e: Exception) {
                sendEvent(FixedIncomeUiEvent.ShowMessage("操作失败: ${e.message}"))
            }
        }
    }

    // ========== 类型转换 ==========

    private fun FixedIncomeEntity.toDomainModel(): FixedIncome {
        return FixedIncome(
            id = id,
            name = name,
            amount = amount,
            type = type.toDomainType(),
            frequency = frequency.toDomainFrequency(),
            startDate = startDate,
            endDate = endDate,
            isActive = isActive
        )
    }

    private fun EntityType.toDomainType(): FixedIncomeType {
        return when (this) {
            EntityType.INCOME -> FixedIncomeType.INCOME
            EntityType.EXPENSE -> FixedIncomeType.EXPENSE
        }
    }

    private fun EntityFrequency.toDomainFrequency(): FixedIncomeFrequency {
        return when (this) {
            EntityFrequency.DAILY -> FixedIncomeFrequency.DAILY
            EntityFrequency.WEEKLY -> FixedIncomeFrequency.WEEKLY
            EntityFrequency.MONTHLY -> FixedIncomeFrequency.MONTHLY
            EntityFrequency.YEARLY -> FixedIncomeFrequency.YEARLY
        }
    }

    private fun FixedIncomeType.toEntityType(): EntityType {
        return when (this) {
            FixedIncomeType.INCOME -> EntityType.INCOME
            FixedIncomeType.EXPENSE -> EntityType.EXPENSE
        }
    }

    private fun FixedIncomeFrequency.toEntityFrequency(): EntityFrequency {
        return when (this) {
            FixedIncomeFrequency.DAILY -> EntityFrequency.DAILY
            FixedIncomeFrequency.WEEKLY -> EntityFrequency.WEEKLY
            FixedIncomeFrequency.MONTHLY -> EntityFrequency.MONTHLY
            FixedIncomeFrequency.YEARLY -> EntityFrequency.YEARLY
        }
    }
}

