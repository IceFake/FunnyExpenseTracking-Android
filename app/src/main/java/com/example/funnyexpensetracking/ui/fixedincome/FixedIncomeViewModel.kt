package com.example.funnyexpensetracking.ui.fixedincome

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeEntity
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency as EntityFrequency
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType as EntityType
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency
import com.example.funnyexpensetracking.domain.model.FixedIncomeType
import com.example.funnyexpensetracking.domain.usecase.RealtimeAssetCalculator
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 固定收支ViewModel
 *
 * 功能逻辑：
 * - 新增条目时，必填项（名称，金额，类型，频率，开始日期），选填项（结束日期）
 * - 条目新增后不可编辑，但可以长按条目选择停用或者删除
 */
@HiltViewModel
class FixedIncomeViewModel @Inject constructor(
    private val fixedIncomeDao: FixedIncomeDao,
    private val realtimeAssetCalculator: RealtimeAssetCalculator
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
                val currentTime = getCurrentMinuteTimestamp()
                val fixedIncomes = entities.map { it.toDomainModel() }

                // 计算每分钟收入/支出（只统计当前生效的条目）
                val incomePerMinute = fixedIncomes
                    .filter { it.type == FixedIncomeType.INCOME && it.isEffectiveAt(currentTime) }
                    .sumOf { it.getAmountPerMinute() }

                val expensePerMinute = fixedIncomes
                    .filter { it.type == FixedIncomeType.EXPENSE && it.isEffectiveAt(currentTime) }
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
        updateState { copy(showAddDialog = true) }
    }

    /**
     * 隐藏添加弹窗
     */
    fun hideAddDialog() {
        updateState { copy(showAddDialog = false) }
        sendEvent(FixedIncomeUiEvent.DismissDialog)
    }

    /**
     * 添加固定收支
     * @param name 名称
     * @param amount 周期金额
     * @param type 类型（收入/支出）
     * @param frequency 频率（DAILY/WEEKLY/MONTHLY/YEARLY）
     * @param startDate 开始日期（精确至分钟）
     * @param endDate 结束日期（精确至分钟，可为空表示持续）
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
                val currentTime = getCurrentMinuteTimestamp()

                val entity = FixedIncomeEntity(
                    name = name,
                    amount = amount,
                    type = type.toEntityType(),
                    frequency = frequency.toEntityFrequency(),
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true,
                    accumulatedMinutes = 0,
                    accumulatedAmount = 0.0,
                    lastRecordTime = currentTime,  // 初始化为当前时间
                    createdAt = System.currentTimeMillis()
                )
                fixedIncomeDao.insert(entity)

                // 重新计算资产
                realtimeAssetCalculator.recalculateAsset()

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
     * 删除固定收支
     */
    fun deleteFixedIncome(fixedIncome: FixedIncome) {
        viewModelScope.launch {
            try {
                fixedIncomeDao.deleteById(fixedIncome.id)

                // 重新计算资产
                realtimeAssetCalculator.recalculateAsset()

                sendEvent(FixedIncomeUiEvent.FixedIncomeDeleted)
                sendEvent(FixedIncomeUiEvent.ShowMessage("删除成功"))
            } catch (e: Exception) {
                sendEvent(FixedIncomeUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 停用固定收支
     */
    fun deactivateFixedIncome(fixedIncome: FixedIncome) {
        viewModelScope.launch {
            try {
                fixedIncomeDao.updateActiveStatus(fixedIncome.id, false)

                // 重新计算资产
                realtimeAssetCalculator.recalculateAsset()

                sendEvent(FixedIncomeUiEvent.ShowMessage("已停用「${fixedIncome.name}」"))
            } catch (e: Exception) {
                sendEvent(FixedIncomeUiEvent.ShowMessage("操作失败: ${e.message}"))
            }
        }
    }

    /**
     * 获取当前分钟的时间戳
     */
    private fun getCurrentMinuteTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
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
            isActive = isActive,
            accumulatedMinutes = accumulatedMinutes,
            accumulatedAmount = accumulatedAmount,
            lastRecordTime = lastRecordTime
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

