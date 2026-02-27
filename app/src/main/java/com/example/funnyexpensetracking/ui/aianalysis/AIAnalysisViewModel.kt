package com.example.funnyexpensetracking.ui.aianalysis

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.repository.DeepSeekAnalysisRepositoryImpl
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
    private val aiAnalysisRepository: AIAnalysisRepository,
    private val userPreferencesManager: UserPreferencesManager
) : BaseViewModel<AIAnalysisUiState, AIAnalysisUiEvent>() {

    override fun initialState() = AIAnalysisUiState()

    init {
        // 初始化时加载当前API Key（不能在initialState中访问，因为父类构造先于字段初始化）
        updateState { copy(currentApiKey = userPreferencesManager.getDeepSeekApiKey()) }
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
                    val errorMessage = result.message ?: "分析失败"
                    val isApiKeyError = errorMessage == DeepSeekAnalysisRepositoryImpl.ERROR_API_KEY_MISSING
                            || errorMessage == DeepSeekAnalysisRepositoryImpl.ERROR_API_KEY_INVALID

                    updateState {
                        copy(
                            isAnalyzing = false,
                            loadingState = LoadingState.ERROR,
                            errorMessage = when (errorMessage) {
                                DeepSeekAnalysisRepositoryImpl.ERROR_API_KEY_MISSING ->
                                    "未配置API Key，请点击左下角设置按钮添加"
                                DeepSeekAnalysisRepositoryImpl.ERROR_API_KEY_INVALID ->
                                    "API Key无效，请点击左下角设置按钮重新配置"
                                else -> errorMessage
                            }
                        )
                    }

                    if (isApiKeyError) {
                        // API Key 缺失或无效时自动弹出设置弹窗
                        sendEvent(AIAnalysisUiEvent.ShowApiKeyDialog)
                    } else {
                        sendEvent(AIAnalysisUiEvent.ShowMessage("分析失败: $errorMessage"))
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * 打开API Key设置弹窗
     */
    fun openApiKeyDialog() {
        updateState {
            copy(
                showApiKeyDialog = true,
                currentApiKey = userPreferencesManager.getDeepSeekApiKey()
            )
        }
    }

    /**
     * 关闭API Key设置弹窗
     */
    fun dismissApiKeyDialog() {
        updateState { copy(showApiKeyDialog = false) }
    }

    /**
     * 保存API Key并自动重试分析
     */
    fun saveApiKey(apiKey: String) {
        userPreferencesManager.saveDeepSeekApiKey(apiKey.trim())
        updateState {
            copy(
                showApiKeyDialog = false,
                currentApiKey = apiKey.trim()
            )
        }
        sendEvent(AIAnalysisUiEvent.ShowMessage("API Key已保存"))
        // 保存后自动重试分析
        requestAnalysis()
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
                    // 获取建议失败不弹出提示，避免干扰
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

