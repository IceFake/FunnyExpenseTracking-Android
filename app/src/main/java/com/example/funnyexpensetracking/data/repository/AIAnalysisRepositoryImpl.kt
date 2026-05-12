package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.remote.api.AIAnalysisApiService
import com.example.funnyexpensetracking.data.remote.dto.AIAnalysisRequest
import com.example.funnyexpensetracking.data.remote.dto.AIAnalysisResultDto
import com.example.funnyexpensetracking.data.remote.dto.FixedIncomeDto
import com.example.funnyexpensetracking.data.remote.dto.HabitInsightDto
import com.example.funnyexpensetracking.data.remote.dto.PredictionDto
import com.example.funnyexpensetracking.data.remote.dto.SuggestionDto
import com.example.funnyexpensetracking.data.remote.dto.TransactionDto
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.AIAnalysisRepository
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType as EntityFixedIncomeType

/**
 * 通过业务后端代理调用 DeepSeek（密钥在后端，不在 App 内）。
 */
@Singleton
class AIAnalysisRepositoryImpl @Inject constructor(
    private val aiAnalysisApiService: AIAnalysisApiService,
    private val transactionDao: TransactionDao,
    private val fixedIncomeDao: FixedIncomeDao,
    private val userPreferencesManager: UserPreferencesManager
) : AIAnalysisRepository {

    override suspend fun analyzeHabits(): Resource<AIAnalysisResult> {
        return try {
            val transactions = transactionDao.getAllTransactions().first()
            val fixedIncomes = fixedIncomeDao.getAllActiveFixedIncomes().first()

            val userId = userPreferencesManager.getBackendUserId()
                .ifBlank { userPreferencesManager.getBackendUserEmail() }
                .ifBlank { userPreferencesManager.getOrCreateDeviceId() }

            val request = AIAnalysisRequest(
                userId = userId,
                transactions = transactions.map { it.toAnalysisWireDto() },
                fixedIncomes = fixedIncomes.map { entity ->
                    FixedIncomeDto(
                        id = entity.id,
                        name = entity.name,
                        amount = entity.amount,
                        type = if (entity.type == EntityFixedIncomeType.INCOME) "income" else "expense",
                        frequency = entity.frequency.name.lowercase(),
                        startDate = null,
                        endDate = null,
                        isActive = entity.isActive,
                        accumulatedMinutes = entity.accumulatedMinutes,
                        accumulatedAmount = entity.accumulatedAmount,
                        lastRecordTime = entity.lastRecordTime,
                        createdAt = entity.createdAt,
                        updatedAt = null
                    )
                },
                analysisType = "habit"
            )

            val response = aiAnalysisApiService.analyzeHabits(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomainModel())
            } else {
                Resource.Error(response.body()?.message ?: response.message() ?: "AI分析请求失败")
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
                Resource.Error(response.body()?.message ?: response.message() ?: "获取建议失败")
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
                Resource.Error(response.body()?.message ?: response.message() ?: "获取历史分析失败")
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
                Resource.Error(response.body()?.message ?: response.message() ?: "获取分析结果失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    private fun com.example.funnyexpensetracking.data.local.entity.TransactionEntity.toAnalysisWireDto(): TransactionDto {
        return TransactionDto(
            id = serverId?.toLongOrNull() ?: id,
            amount = amount,
            type = type.name,
            category = category,
            note = note,
            date = date,
            createdAt = createdAt,
            updatedAt = updatedAt,
            accountId = accountId
        )
    }

    private fun AIAnalysisResultDto.toDomainModel(): AIAnalysisResult {
        val habitDtos = spendingHabits ?: insights.orEmpty()
        return AIAnalysisResult(
            analysisId = analysisId ?: id.orEmpty(),
            summary = summary.orEmpty(),
            spendingHabits = habitDtos.map { it.toDomainHabit() },
            suggestions = suggestions.orEmpty().map { it.toDomainSuggestion() },
            predictions = predictions?.toDomainPrediction(),
            generatedAt = generatedAt ?: createdAt ?: System.currentTimeMillis()
        )
    }

    private fun HabitInsightDto.toDomainHabit(): HabitInsight {
        val text = insight?.takeIf { it.isNotBlank() }
            ?: recommendation?.takeIf { it.isNotBlank() }
            ?: buildString {
                monthlyAverage?.let { append("月均 ${it}") }
                percentageChange?.let { append(if (isNotEmpty()) "，" else ""); append("环比 ${it}%") }
            }.ifBlank { "—" }
        return HabitInsight(
            category = category,
            insight = text,
            trend = when ((trend ?: "").lowercase()) {
                "increasing" -> HabitTrend.INCREASING
                "decreasing" -> HabitTrend.DECREASING
                else -> HabitTrend.STABLE
            }
        )
    }

    private fun SuggestionDto.toDomainSuggestion(): Suggestion {
        return Suggestion(
            title = title,
            description = description,
            priority = when (priority.lowercase()) {
                "high" -> SuggestionPriority.HIGH
                "medium" -> SuggestionPriority.MEDIUM
                else -> SuggestionPriority.LOW
            }
        )
    }

    private fun PredictionDto.toDomainPrediction(): Prediction {
        return Prediction(
            nextMonthExpense = nextMonthExpense ?: predictedExpense ?: 0.0,
            nextMonthIncome = nextMonthIncome ?: predictedIncome ?: 0.0,
            savingsPotential = savingsPotential ?: 0.0
        )
    }
}
