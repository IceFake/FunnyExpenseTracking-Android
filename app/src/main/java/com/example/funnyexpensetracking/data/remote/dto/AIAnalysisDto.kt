package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * AI 分析请求（与后端 Jackson camelCase 对齐）
 */
data class AIAnalysisRequest(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("transactions") val transactions: List<TransactionDto>,
    @SerializedName("fixedIncomes") val fixedIncomes: List<FixedIncomeDto>,
    /** 与后端分析类型名称一致，例如 SPENDING_HABITS */
    @SerializedName("analysisType") val analysisType: String
)

/**
 * 固定收支 DTO（用于 AI 分析请求）
 */
data class FixedIncomeDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("startDate") val startDate: Long? = null,
    @SerializedName("endDate") val endDate: Long? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("accumulatedMinutes") val accumulatedMinutes: Long? = null,
    @SerializedName("accumulatedAmount") val accumulatedAmount: Double? = null,
    @SerializedName("lastRecordTime") val lastRecordTime: Long? = null,
    @SerializedName("createdAt") val createdAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null
)

/**
 * AI 分析结果 DTO（兼容后端当前返回字段）
 */
data class AIAnalysisResultDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("analysisId") val analysisId: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("analysisType") val analysisType: String? = null,
    val summary: String? = null,
    /** 后端习惯洞察列表字段名 */
    val insights: List<HabitInsightDto>? = null,
    @SerializedName("spending_habits") val spendingHabits: List<HabitInsightDto>? = null,
    val suggestions: List<SuggestionDto>? = null,
    val predictions: List<PredictionDto>? = null,
    @SerializedName("createdAt") val createdAt: Long? = null,
    @SerializedName("generatedAt") val generatedAt: Long? = null,
    @SerializedName("generated_at") val generatedAtSnake: Long? = null
)

/**
 * 习惯洞察 DTO（兼容后端 [HabitInsightDto]）
 */
data class HabitInsightDto(
    @SerializedName("category") val category: String,
    @SerializedName("insight") val insight: String? = null,
    @SerializedName("recommendation") val recommendation: String? = null,
    @SerializedName("trend") val trend: String? = null,
    @SerializedName("monthlyAverage") val monthlyAverage: Double? = null,
    @SerializedName("percentageChange") val percentageChange: Double? = null
)

/**
 * 建议DTO
 */
data class SuggestionDto(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("priority") val priority: String
)

/**
 * 预测 DTO（兼容后端单对象或列表）
 */
data class PredictionDto(
    @SerializedName("next_month_expense") val nextMonthExpense: Double? = null,
    @SerializedName("next_month_income") val nextMonthIncome: Double? = null,
    @SerializedName("savings_potential") val savingsPotential: Double? = null,
    @SerializedName("predictedExpense") val predictedExpense: Double? = null,
    @SerializedName("predictedIncome") val predictedIncome: Double? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("month") val month: String? = null
)
