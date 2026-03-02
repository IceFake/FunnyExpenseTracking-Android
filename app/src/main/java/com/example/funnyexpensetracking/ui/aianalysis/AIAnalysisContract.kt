package com.example.funnyexpensetracking.ui.aianalysis

import com.example.funnyexpensetracking.domain.model.AIAnalysisResult
import com.example.funnyexpensetracking.domain.model.Suggestion
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState
import com.example.funnyexpensetracking.ui.common.ErrorState

/**
 * AI分析页面状态
 */
data class AIAnalysisUiState(
    val analysisResult: AIAnalysisResult? = null,
    val suggestions: List<Suggestion> = emptyList(),
    val historyResults: List<AIAnalysisResult> = emptyList(),
    val loadingState: LoadingState = LoadingState.IDLE,
    val isAnalyzing: Boolean = false,
    override val errorMessage: String? = null,
    val showApiKeyDialog: Boolean = false,
    val currentApiKey: String = ""
) : UiState, ErrorState

/**
 * AI分析页面事件
 */
sealed class AIAnalysisUiEvent : UiEvent {
    data class ShowMessage(val message: String) : AIAnalysisUiEvent()
    object AnalysisCompleted : AIAnalysisUiEvent()
    object ShowApiKeyDialog : AIAnalysisUiEvent()
}

