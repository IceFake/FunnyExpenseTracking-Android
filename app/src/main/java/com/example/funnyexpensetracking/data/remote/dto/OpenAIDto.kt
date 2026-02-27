package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * OpenAI/DeepSeek Chat Completion 请求
 * DeepSeek API 兼容 OpenAI 格式
 */
data class OpenAIChatRequest(
    @SerializedName("model") val model: String = "deepseek-chat",
    @SerializedName("messages") val messages: List<OpenAIChatMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 1000,
    @SerializedName("stream") val stream: Boolean = false,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

/**
 * 响应格式配置
 * 设置为 {"type": "json_object"} 可强制AI返回JSON格式
 */
data class ResponseFormat(
    @SerializedName("type") val type: String = "json_object"
)

/**
 * OpenAI 聊天消息
 */
data class OpenAIChatMessage(
    @SerializedName("role") val role: String,  // system, user, assistant
    @SerializedName("content") val content: String
)

/**
 * OpenAI Chat Completion 响应
 */
data class OpenAIChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("object") val objectType: String?,
    @SerializedName("created") val created: Long?,
    @SerializedName("model") val model: String?,
    @SerializedName("choices") val choices: List<OpenAIChatChoice>?,
    @SerializedName("usage") val usage: OpenAIUsage?,
    @SerializedName("error") val error: OpenAIError?
)

/**
 * OpenAI 选择项
 */
data class OpenAIChatChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: OpenAIChatMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

/**
 * OpenAI Token使用情况
 */
data class OpenAIUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

/**
 * OpenAI 错误响应
 */
data class OpenAIError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("code") val code: String?
)

/**
 * 解析后的AI消费分析结果
 */
data class ParsedAIAnalysis(
    val summary: String,
    val habits: List<ParsedHabit>,
    val suggestions: List<ParsedSuggestion>,
    val prediction: ParsedPrediction?
)

data class ParsedHabit(
    val category: String,
    val insight: String,
    val trend: String // increasing, stable, decreasing
)

data class ParsedSuggestion(
    val title: String,
    val description: String,
    val priority: String // high, medium, low
)

data class ParsedPrediction(
    val nextMonthExpense: Double,
    val nextMonthIncome: Double,
    val savingsPotential: Double
)

