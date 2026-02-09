package com.example.funnyexpensetracking.ui.asset

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.dao.InvestmentDao
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.StockHolding
import com.example.funnyexpensetracking.domain.repository.AssetRepository
import com.example.funnyexpensetracking.domain.repository.StockRepository
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
    private val assetRepository: AssetRepository,
    private val stockRepository: StockRepository,
    private val investmentDao: InvestmentDao
) : BaseViewModel<AssetUiState, AssetUiEvent>() {

    private var realtimeUpdateJob: Job? = null

    override fun initialState() = AssetUiState()

    init {
        loadAssetData()
        startRealtimeAssetUpdate()
        observeInvestmentChanges()
    }

    /**
     * 加载资产数据
     */
    fun loadAssetData() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        // 加载固定收支
        assetRepository.getAllActiveFixedIncomes()
            .onEach { fixedIncomes ->
                updateState { copy(fixedIncomes = fixedIncomes) }
            }
            .launchIn(viewModelScope)

        // 加载股票持仓
        stockRepository.getAllStockHoldings()
            .onEach { holdings ->
                updateState { copy(stockHoldings = holdings) }
            }
            .launchIn(viewModelScope)

        // 计算初始资产
        viewModelScope.launch {
            val summary = assetRepository.calculateCurrentAssetSummary()
            updateState {
                copy(
                    assetSummary = summary,
                    loadingState = LoadingState.SUCCESS
                )
            }
        }
    }

    /**
     * 监听投资数据变化，自动刷新总资产
     */
    private fun observeInvestmentChanges() {
        // 监听投资当前价值变化
        investmentDao.getTotalCurrentValueFlow()
            .onEach {
                // 投资价值变化时重新计算总资产
                val summary = assetRepository.calculateCurrentAssetSummary()
                updateState { copy(assetSummary = summary) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 手动刷新资产数据
     */
    fun refreshAssetSummary() {
        viewModelScope.launch {
            val summary = assetRepository.calculateCurrentAssetSummary()
            updateState { copy(assetSummary = summary) }
        }
    }

    /**
     * 开始实时资产更新（每分钟更新一次）
     */
    private fun startRealtimeAssetUpdate() {
        realtimeUpdateJob?.cancel()
        realtimeUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000) // 每分钟更新
                val summary = assetRepository.calculateCurrentAssetSummary()
                updateState { copy(assetSummary = summary) }
            }
        }
    }

    /**
     * 添加固定收支
     */
    fun addFixedIncome(fixedIncome: FixedIncome) {
        viewModelScope.launch {
            try {
                assetRepository.addFixedIncome(fixedIncome)
                sendEvent(AssetUiEvent.ShowMessage("添加成功"))
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
                assetRepository.deleteFixedIncome(fixedIncome)
                sendEvent(AssetUiEvent.ShowMessage("删除成功"))
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
                stockRepository.addStockHolding(stock)
                sendEvent(AssetUiEvent.ShowMessage("添加成功"))
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
                stockRepository.deleteStockHolding(stock)
                sendEvent(AssetUiEvent.ShowMessage("删除成功"))
            } catch (e: Exception) {
                sendEvent(AssetUiEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * 刷新股票价格
     */
    fun refreshStockPrices() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            when (val result = stockRepository.refreshAllStockPrices()) {
                is Resource.Success -> {
                    sendEvent(AssetUiEvent.ShowMessage("刷新成功"))
                }
                is Resource.Error -> {
                    sendEvent(AssetUiEvent.ShowMessage("刷新失败: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
            updateState { copy(isRefreshing = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeUpdateJob?.cancel()
    }
}

