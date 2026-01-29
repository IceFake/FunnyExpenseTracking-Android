package com.example.funnyexpensetracking.ui.aianalysis

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.usecase.ai.*
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI分析ViewModel
 */
@HiltViewModel
class AIAnalysisViewModel @Inject constructor(
    private val getAIAnalysisUseCase: GetAIAnalysisUseCase,
    private val getAISuggestionsUseCase: GetAISuggestionsUseCase,
    private val getAnalysisHistoryUseCase: GetAnalysisHistoryUseCase
) : BaseViewModel<AIAnalysisUiState, AIAnalysisUiEvent>() {

    override fun initialState() = AIAnalysisUiState()

    init {
        loadSuggestions()
        loadAnalysisHistory()
    }

    /**
     * 请求AI分析
     */
    fun requestAnalysis() {
        updateState { copy(isAnalyzing = true, loadingState = LoadingState.LOADING) }

        viewModelScope.launch {
            when (val result = getAIAnalysisUseCase()) {
                is Resource.Success -> {
                    updateState {
                        copy(
                            analysisResult = result.data,
                            isAnalyzing = false,
                            loadingState = LoadingState.SUCCESS
                        )
                    }
                    sendEvent(AIAnalysisUiEvent.AnalysisCompleted)
                    sendEvent(AIAnalysisUiEvent.ShowMessage("分析完成"))
                    // 刷新历史记录
                    loadAnalysisHistory()
                }
                is Resource.Error -> {
                    updateState {
                        copy(
                            isAnalyzing = false,
                            loadingState = LoadingState.ERROR,
                            errorMessage = result.message
                        )
                    }
                    sendEvent(AIAnalysisUiEvent.ShowMessage(result.message ?: "分析失败"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 加载AI建议
     */
    fun loadSuggestions() {
        viewModelScope.launch {
            when (val result = getAISuggestionsUseCase()) {
                is Resource.Success -> {
                    updateState { copy(suggestions = result.data ?: emptyList()) }
                }
                is Resource.Error -> {
                    // 加载建议失败不影响主页面
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 加载历史分析记录
     */
    fun loadAnalysisHistory(limit: Int = 10) {
        viewModelScope.launch {
            when (val result = getAnalysisHistoryUseCase(limit)) {
                is Resource.Success -> {
                    updateState { copy(historyResults = result.data ?: emptyList()) }
                }
                is Resource.Error -> {
                    // 加载历史失败不影响主页面
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 清除当前分析结果
     */
    fun clearAnalysisResult() {
        updateState { copy(analysisResult = null) }
    }
}

