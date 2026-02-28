package com.example.funnyexpensetracking.ui.financialquery

import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.remote.dto.OpenAIChatMessage
import com.example.funnyexpensetracking.data.repository.FinancialQueryRepositoryImpl
import com.example.funnyexpensetracking.domain.model.ChatMessage
import com.example.funnyexpensetracking.domain.model.ChatRole
import com.example.funnyexpensetracking.domain.repository.FinancialQueryRepository
import com.example.funnyexpensetracking.ui.common.BaseViewModel
import com.example.funnyexpensetracking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 自然语言财务查询ViewModel
 * 管理聊天消息列表、发送查询请求、处理AI回复
 */
@HiltViewModel
class FinancialQueryViewModel @Inject constructor(
    private val financialQueryRepository: FinancialQueryRepository,
    private val userPreferencesManager: UserPreferencesManager
) : BaseViewModel<FinancialQueryUiState, FinancialQueryUiEvent>() {

    override fun initialState() = FinancialQueryUiState()

    init {
        updateState { copy(currentApiKey = userPreferencesManager.getDeepSeekApiKey()) }
        // 添加欢迎消息
        addWelcomeMessage()
    }

    /**
     * 添加欢迎消息
     */
    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            role = ChatRole.ASSISTANT,
            content = "👋 你好！我是趣味记账AI助手。\n\n你可以用自然语言向我提问关于你的财务状况，比如：\n• 这个月花了多少钱？\n• 我最大的支出是什么？\n• 给我一些理财建议\n\n也可以点击下方的快捷问题开始对话～"
        )
        updateState { copy(messages = listOf(welcomeMessage)) }
    }

    /**
     * 发送查询
     */
    fun sendQuery(question: String) {
        if (question.isBlank()) return
        if (currentState().isLoading) return

        // 添加用户消息
        val userMessage = ChatMessage(role = ChatRole.USER, content = question.trim())
        updateState {
            copy(
                messages = messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }
        sendEvent(FinancialQueryUiEvent.ScrollToBottom)

        // 构建对话历史（用于上下文关联）
        val conversationHistory = buildConversationHistory()

        viewModelScope.launch {
            when (val result = financialQueryRepository.queryFinancial(question.trim(), conversationHistory)) {
                is Resource.Success -> {
                    val aiContent = result.data ?: "抱歉，我无法生成回复。"
                    val aiMessage = ChatMessage(role = ChatRole.ASSISTANT, content = aiContent)
                    updateState {
                        copy(
                            messages = messages + aiMessage,
                            isLoading = false
                        )
                    }
                    sendEvent(FinancialQueryUiEvent.ScrollToBottom)
                }
                is Resource.Error -> {
                    val errorMsg = result.message ?: "查询失败"
                    val isApiKeyError = errorMsg == FinancialQueryRepositoryImpl.ERROR_API_KEY_MISSING
                            || errorMsg == FinancialQueryRepositoryImpl.ERROR_API_KEY_INVALID

                    if (isApiKeyError) {
                        val errorContent = when (errorMsg) {
                            FinancialQueryRepositoryImpl.ERROR_API_KEY_MISSING ->
                                "⚠️ 未配置API Key，请先设置DeepSeek API Key后再使用。"
                            FinancialQueryRepositoryImpl.ERROR_API_KEY_INVALID ->
                                "⚠️ API Key无效，请重新设置正确的DeepSeek API Key。"
                            else -> "⚠️ $errorMsg"
                        }
                        val errorMessage = ChatMessage(role = ChatRole.ASSISTANT, content = errorContent)
                        updateState {
                            copy(
                                messages = messages + errorMessage,
                                isLoading = false
                            )
                        }
                        sendEvent(FinancialQueryUiEvent.ShowApiKeyDialog)
                    } else {
                        val errorMessage = ChatMessage(
                            role = ChatRole.ASSISTANT,
                            content = "❌ $errorMsg\n\n请稍后重试。"
                        )
                        updateState {
                            copy(
                                messages = messages + errorMessage,
                                isLoading = false
                            )
                        }
                        sendEvent(FinancialQueryUiEvent.ShowMessage(errorMsg))
                    }
                    sendEvent(FinancialQueryUiEvent.ScrollToBottom)
                }
                is Resource.Loading -> { }
            }
        }
    }

    /**
     * 构建对话历史（用于发送给API的上下文）
     * 排除欢迎消息，最多保留最近5轮对话
     */
    private fun buildConversationHistory(): List<OpenAIChatMessage> {
        val messages = currentState().messages
        // 跳过第一条欢迎消息，取最后10条（5轮对话）
        return messages
            .drop(1) // 跳过欢迎消息
            .takeLast(10)
            .dropLast(1) // 去掉最后一条（即刚添加的用户消息，会在Repository中单独添加）
            .map { msg ->
                OpenAIChatMessage(
                    role = when (msg.role) {
                        ChatRole.USER -> "user"
                        ChatRole.ASSISTANT -> "assistant"
                        ChatRole.SYSTEM -> "system"
                    },
                    content = msg.content
                )
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
     * 保存API Key并关闭弹窗
     */
    fun saveApiKey(apiKey: String) {
        userPreferencesManager.saveDeepSeekApiKey(apiKey.trim())
        updateState {
            copy(
                showApiKeyDialog = false,
                currentApiKey = apiKey.trim()
            )
        }
        sendEvent(FinancialQueryUiEvent.ShowMessage("API Key已保存"))
    }

    /**
     * 关闭API Key弹窗
     */
    fun dismissApiKeyDialog() {
        updateState { copy(showApiKeyDialog = false) }
    }

    /**
     * 清空对话
     */
    fun clearChat() {
        addWelcomeMessage()
    }
}

