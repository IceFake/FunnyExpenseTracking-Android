package com.example.funnyexpensetracking.ui.asset

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.StockHolding
import com.example.funnyexpensetracking.domain.usecase.asset.*
import com.example.funnyexpensetracking.domain.usecase.stock.*
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
 * 资产管理ViewModel
 */
@HiltViewModel
class AssetViewModel @Inject constructor(
    private val calculateRealtimeAssetUseCase: CalculateRealtimeAssetUseCase,
    private val addFixedIncomeUseCase: AddFixedIncomeUseCase,
    private val getFixedIncomesUseCase: GetFixedIncomesUseCase,
    private val deleteFixedIncomeUseCase: DeleteFixedIncomeUseCase,
    private val getStockHoldingsUseCase: GetStockHoldingsUseCase,
    private val addStockHoldingUseCase: AddStockHoldingUseCase,
    private val deleteStockHoldingUseCase: DeleteStockHoldingUseCase,
    private val syncStockPriceUseCase: SyncStockPriceUseCase
) : BaseViewModel<AssetUiState, AssetUiEvent>() {

    private var realtimeUpdateJob: Job? = null

    override fun initialState() = AssetUiState()

    init {
        loadAssetData()
        startRealtimeAssetUpdate()
    }

    /**
     * 加载资产数据
     */
    fun loadAssetData() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        // 加载固定收支
        getFixedIncomesUseCase()
            .onEach { fixedIncomes ->
                updateState { copy(fixedIncomes = fixedIncomes) }
            }
            .launchIn(viewModelScope)

        // 加载股票持仓
        getStockHoldingsUseCase()
            .onEach { holdings ->
                updateState { copy(stockHoldings = holdings) }
            }
            .launchIn(viewModelScope)

        // 计算资产汇总
        viewModelScope.launch {
            try {
                val summary = calculateRealtimeAssetUseCase()
                updateState {
                    copy(
                        assetSummary = summary,
                        realtimeTotalAsset = summary.totalAsset,
                        loadingState = LoadingState.SUCCESS,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        loadingState = LoadingState.ERROR,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    /**
     * 启动实时资产更新（每分钟更新一次）
     */
    private fun startRealtimeAssetUpdate() {
        realtimeUpdateJob?.cancel()
        realtimeUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000) // 每分钟更新
                updateRealtimeAsset()
            }
        }
    }

    /**
     * 更新实时资产
     */
    private suspend fun updateRealtimeAsset() {
        val summary = currentState().assetSummary ?: return
        val elapsedMinutes = (System.currentTimeMillis() - currentState().lastUpdateTime) / 60_000.0
        val assetChange = summary.netIncomePerMinute * elapsedMinutes
        val newTotalAsset = summary.totalAsset + assetChange

        updateState {
            copy(
                realtimeTotalAsset = newTotalAsset,
                lastUpdateTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * 刷新资产数据
     */
    fun refreshAssetData() {
        updateState { copy(isRefreshing = true) }

        viewModelScope.launch {
            // 先刷新股票价格
            syncStockPriceUseCase()

            // 重新计算资产
            try {
                val summary = calculateRealtimeAssetUseCase()
                updateState {
                    copy(
                        assetSummary = summary,
                        realtimeTotalAsset = summary.totalAsset,
                        isRefreshing = false,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }
                sendEvent(AssetUiEvent.ShowMessage("刷新成功"))
            } catch (e: Exception) {
                updateState { copy(isRefreshing = false) }
                sendEvent(AssetUiEvent.ShowMessage("刷新失败: ${e.message}"))
            }
        }
    }

    /**
     * 添加固定收支
     */
    fun addFixedIncome(fixedIncome: FixedIncome) {
        viewModelScope.launch {
            try {
                addFixedIncomeUseCase(fixedIncome)
                sendEvent(AssetUiEvent.FixedIncomeAdded)
                sendEvent(AssetUiEvent.ShowMessage("添加成功"))
                refreshAssetData()
            } catch (e: Exception) {
                sendEvent(AssetUiEvent.ShowMessage("添加失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除固定收支
     */
    fun deleteFixedIncome(fixedIncome: FixedIncome) {
        viewModelScope.launch {
            try {
                deleteFixedIncomeUseCase(fixedIncome)
                sendEvent(AssetUiEvent.ShowMessage("删除成功"))
                refreshAssetData()
            } catch (e: Exception) {
                sendEvent(AssetUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 添加股票持仓
     */
    fun addStockHolding(stock: StockHolding) {
        viewModelScope.launch {
            try {
                addStockHoldingUseCase(stock)
                sendEvent(AssetUiEvent.StockAdded)
                sendEvent(AssetUiEvent.ShowMessage("添加成功"))
                refreshAssetData()
            } catch (e: Exception) {
                sendEvent(AssetUiEvent.ShowMessage("添加失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除股票持仓
     */
    fun deleteStockHolding(stock: StockHolding) {
        viewModelScope.launch {
            try {
                deleteStockHoldingUseCase(stock)
                sendEvent(AssetUiEvent.ShowMessage("删除成功"))
                refreshAssetData()
            } catch (e: Exception) {
                sendEvent(AssetUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 同步股票价格
     */
    fun syncStockPrices() {
        viewModelScope.launch {
            when (val result = syncStockPriceUseCase()) {
                is Resource.Success -> {
                    sendEvent(AssetUiEvent.ShowMessage("股票价格已更新"))
                    refreshAssetData()
                }
                is Resource.Error -> {
                    sendEvent(AssetUiEvent.ShowMessage("更新股票价格失败: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeUpdateJob?.cancel()
    }
}

