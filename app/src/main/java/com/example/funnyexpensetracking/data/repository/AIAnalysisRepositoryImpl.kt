package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.remote.api.AIAnalysisApiService
import com.example.funnyexpensetracking.data.remote.dto.*
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.AIAnalysisRepository
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.example.funnyexpensetracking.data.local.entity.TransactionType as EntityTransactionType
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType as EntityFixedIncomeType

/**
 * AI分析Repository实现类
 */
@Singleton
class AIAnalysisRepositoryImpl @Inject constructor(
    private val aiAnalysisApiService: AIAnalysisApiService,
    private val transactionDao: TransactionDao,
    private val fixedIncomeDao: FixedIncomeDao
) : AIAnalysisRepository {

    override suspend fun analyzeHabits(): Resource<AIAnalysisResult> {
        return try {
            val transactions = transactionDao.getAllTransactions().first()
            val fixedIncomes = fixedIncomeDao.getAllActiveFixedIncomes().first()

            val request = AIAnalysisRequest(
                userId = "current_user",
                transactions = transactions.map { entity ->
                    TransactionDto(
                        id = entity.id,
                        amount = entity.amount,
                        type = if (entity.type == EntityTransactionType.INCOME) "income" else "expense",
                        category = entity.category,
                        note = entity.note,
                        date = entity.date,
                        createdAt = entity.createdAt
                    )
                },
                fixedIncomes = fixedIncomes.map { entity ->
                    FixedIncomeDto(
                        id = entity.id,
                        name = entity.name,
                        amount = entity.amount,
                        type = if (entity.type == EntityFixedIncomeType.INCOME) "income" else "expense",
                        frequency = entity.frequency.name.lowercase()
                    )
                },
                analysisType = "habit"
            )

            val response = aiAnalysisApiService.analyzeHabits(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomainModel())
            } else {
                Resource.Error(response.message() ?: "AI分析请求失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    override suspend fun getSuggestions(): Resource<List<Suggestion>> {
        return try {
            val response = aiAnalysisApiService.getSuggestions()
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(
                    response.body()!!.data!!.map { dto ->
                        Suggestion(
                            title = dto.title,
                            description = dto.description,
                            priority = when (dto.priority) {
                                "high" -> SuggestionPriority.HIGH
                                "medium" -> SuggestionPriority.MEDIUM
                                else -> SuggestionPriority.LOW
                            }
                        )
                    }
                )
            } else {
                Resource.Error(response.message() ?: "获取建议失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    override suspend fun getAnalysisHistory(limit: Int): Resource<List<AIAnalysisResult>> {
        return try {
            val response = aiAnalysisApiService.getAnalysisHistory(limit)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.map { it.toDomainModel() })
            } else {
                Resource.Error(response.message() ?: "获取历史分析失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    override suspend fun getAnalysisById(analysisId: String): Resource<AIAnalysisResult> {
        return try {
            val response = aiAnalysisApiService.getAnalysisResult(analysisId)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomainModel())
            } else {
                Resource.Error(response.message() ?: "获取分析结果失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    private fun AIAnalysisResultDto.toDomainModel(): AIAnalysisResult {
        return AIAnalysisResult(
            analysisId = analysisId,
            summary = summary,
            spendingHabits = spendingHabits.map { habit ->
                HabitInsight(
                    category = habit.category,
                    insight = habit.insight,
                    trend = when (habit.trend) {
                        "increasing" -> HabitTrend.INCREASING
                        "decreasing" -> HabitTrend.DECREASING
                        else -> HabitTrend.STABLE
                    }
                )
            },
            suggestions = suggestions.map { suggestion ->
                Suggestion(
                    title = suggestion.title,
                    description = suggestion.description,
                    priority = when (suggestion.priority) {
                        "high" -> SuggestionPriority.HIGH
                        "medium" -> SuggestionPriority.MEDIUM
                        else -> SuggestionPriority.LOW
                    }
                )
            },
            predictions = predictions?.let {
                Prediction(
                    nextMonthExpense = it.nextMonthExpense,
                    nextMonthIncome = it.nextMonthIncome,
                    savingsPotential = it.savingsPotential
                )
            },
            generatedAt = generatedAt
        )
    }
}

