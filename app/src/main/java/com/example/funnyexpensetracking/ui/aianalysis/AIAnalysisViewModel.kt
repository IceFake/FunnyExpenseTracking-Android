package com.example.funnyexpensetracking.ui.aianalysis

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.repository.AIAnalysisRepository
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
    private val aiAnalysisRepository: AIAnalysisRepository
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
            when (val result = aiAnalysisRepository.analyzeHabits()) {
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
                    sendEvent(AIAnalysisUiEvent.ShowMessage("分析失败: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 加载AI建议
     */
    private fun loadSuggestions() {
        viewModelScope.launch {
            when (val result = aiAnalysisRepository.getSuggestions()) {
                is Resource.Success -> {
                    updateState { copy(suggestions = result.data ?: emptyList()) }
                }
                is Resource.Error -> {
                    sendEvent(AIAnalysisUiEvent.ShowMessage("获取建议失败"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 加载历史分析记录
     */
    private fun loadAnalysisHistory() {
        viewModelScope.launch {
            when (val result = aiAnalysisRepository.getAnalysisHistory()) {
                is Resource.Success -> {
                    updateState { copy(historyResults = result.data ?: emptyList()) }
                }
                is Resource.Error -> {}
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 查看历史分析详情
     */
    fun viewAnalysisDetail(analysisId: String) {
        viewModelScope.launch {
            when (val result = aiAnalysisRepository.getAnalysisById(analysisId)) {
                is Resource.Success -> {
                    updateState { copy(analysisResult = result.data) }
                }
                is Resource.Error -> {
                    sendEvent(AIAnalysisUiEvent.ShowMessage("获取详情失败"))
                }
                is Resource.Loading -> {}
            }
        }
    }
}

