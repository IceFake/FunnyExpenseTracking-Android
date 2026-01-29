package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * AI分析请求
 */
data class AIAnalysisRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("transactions") val transactions: List<TransactionDto>,
    @SerializedName("fixed_incomes") val fixedIncomes: List<FixedIncomeDto>,
    @SerializedName("analysis_type") val analysisType: String
)

/**
 * AI分析结果DTO
 */
data class AIAnalysisResultDto(
    @SerializedName("analysis_id") val analysisId: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("spending_habits") val spendingHabits: List<HabitInsightDto>,
    @SerializedName("suggestions") val suggestions: List<SuggestionDto>,
    @SerializedName("predictions") val predictions: PredictionDto?,
    @SerializedName("generated_at") val generatedAt: Long
)

/**
 * 习惯洞察DTO
 */
data class HabitInsightDto(
    @SerializedName("category") val category: String,
    @SerializedName("insight") val insight: String,
    @SerializedName("trend") val trend: String
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
 * 预测DTO
 */
data class PredictionDto(
    @SerializedName("next_month_expense") val nextMonthExpense: Double,
    @SerializedName("next_month_income") val nextMonthIncome: Double,
    @SerializedName("savings_potential") val savingsPotential: Double
)

