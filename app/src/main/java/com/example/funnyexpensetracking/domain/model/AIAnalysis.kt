package com.example.funnyexpensetracking.domain.model

/**
 * AI分析结果领域模型
 */
data class AIAnalysisResult(
    val analysisId: String,
    val summary: String,
    val spendingHabits: List<HabitInsight>,
    val suggestions: List<Suggestion>,
    val predictions: Prediction?,
    val generatedAt: Long
)

/**
 * 消费习惯洞察
 */
data class HabitInsight(
    val category: String,
    val insight: String,
    val trend: HabitTrend
)

/**
 * 习惯趋势
 */
enum class HabitTrend {
    INCREASING,
    STABLE,
    DECREASING
}

/**
 * AI建议
 */
data class Suggestion(
    val title: String,
    val description: String,
    val priority: SuggestionPriority
)

/**
 * 建议优先级
 */
enum class SuggestionPriority {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * 预测数据
 */
data class Prediction(
    val nextMonthExpense: Double,
    val nextMonthIncome: Double,
    val savingsPotential: Double
)

