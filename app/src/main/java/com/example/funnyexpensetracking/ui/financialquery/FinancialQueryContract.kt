package com.example.funnyexpensetracking.ui.financialquery

import com.example.funnyexpensetracking.domain.model.ChatMessage
import com.example.funnyexpensetracking.ui.common.UiEvent
import com.example.funnyexpensetracking.ui.common.UiState

/**
 * 自然语言财务查询页面状态
 */
data class FinancialQueryUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showApiKeyDialog: Boolean = false,
    val currentApiKey: String = "",
    val suggestedQuestions: List<String> = listOf(
        "这个月我花了多少钱？",
        "我最大的支出类别是什么？",
        "上个月收入是多少？",
        "我的账户余额还有多少？",
        "给我一些省钱建议",
        "我的投资情况怎么样？"
    )
) : UiState

/**
 * 自然语言财务查询页面事件
 */
sealed class FinancialQueryUiEvent : UiEvent {
    data class ShowMessage(val message: String) : FinancialQueryUiEvent()
    object ShowApiKeyDialog : FinancialQueryUiEvent()
    object ScrollToBottom : FinancialQueryUiEvent()
}

