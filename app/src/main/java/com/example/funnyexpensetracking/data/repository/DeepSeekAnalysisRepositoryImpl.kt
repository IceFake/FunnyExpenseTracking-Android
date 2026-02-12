package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.BuildConfig
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.remote.api.DeepSeekApiService
import com.example.funnyexpensetracking.data.remote.dto.*
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.AIAnalysisRepository
import com.example.funnyexpensetracking.util.Resource
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.example.funnyexpensetracking.data.local.entity.TransactionType as EntityTransactionType

/**
 * 使用OpenAI API的AI分析Repository实现类
 * 直接调用OpenAI/ChatGPT API分析用户消费习惯
 */
@Suppress("unused") // fixedIncomeDao 保留供将来扩展使用
@Singleton
class DeepSeekAnalysisRepositoryImpl @Inject constructor(
    private val deepSeekApiService: DeepSeekApiService,
    private val transactionDao: TransactionDao,
    private val fixedIncomeDao: FixedIncomeDao,
    private val gson: Gson
) : AIAnalysisRepository {

    companion object {
        // API密钥通过BuildConfig.DEEPSEEK_API_KEY获取
        // 请在local.properties中添加DEEPSEEK_API_KEY=your_key_here

        private const val SYSTEM_PROMPT = """
你是一个专业的个人财务分析师。请分析用户的消费记录，并以JSON格式返回分析结果。

分析要求：
1. 总结用户的整体消费情况
2. 识别消费习惯和趋势
3. 提供具体的省钱建议
4. 预测下个月的收支情况

请严格按照以下JSON格式返回结果：
{
    "summary": "总体分析摘要（100-200字）",
    "habits": [
        {
            "category": "消费类别（如：餐饮、交通等）",
            "insight": "对该类别消费的洞察",
            "trend": "趋势（increasing/stable/decreasing）"
        }
    ],
    "suggestions": [
        {
            "title": "建议标题",
            "description": "详细建议内容",
            "priority": "优先级（high/medium/low）"
        }
    ],
    "prediction": {
        "nextMonthExpense": 预计下月支出金额,
        "nextMonthIncome": 预计下月收入金额,
        "savingsPotential": 潜在节省金额
    }
}

只返回JSON，不要有其他文字。
"""
    }

    override suspend fun analyzeHabits(): Resource<AIAnalysisResult> {
        return try {
            // 获取最近3个月的交易记录
            val threeMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -3)
            }.timeInMillis

            val transactions = transactionDao.getAllTransactions().first()
                .filter { it.date >= threeMonthsAgo }

            if (transactions.isEmpty()) {
                return Resource.Error("没有足够的交易记录进行分析，请先添加一些消费记录")
            }

            // 构建用户消息
            val userMessage = buildAnalysisPrompt(transactions)

            // 调用OpenAI API
            val request = OpenAIChatRequest(
                model = "deepseek-chat",
                messages = listOf(
                    OpenAIChatMessage(role = "system", content = SYSTEM_PROMPT),
                    OpenAIChatMessage(role = "user", content = userMessage)
                ),
                temperature = 0.7,
                maxTokens = 1500
            )

            val response = deepSeekApiService.createChatCompletion(
                authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!

                // 检查是否有错误
                if (responseBody.error != null) {
                    return Resource.Error("DeepSeek API错误: ${responseBody.error.message}")
                }

                // 提取AI回复内容
                val aiContent = responseBody.choices?.firstOrNull()?.message?.content
                    ?: return Resource.Error("AI未返回有效回复")

                // 解析JSON结果
                val analysisResult = parseAIResponse(aiContent)
                Resource.Success(analysisResult)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("API请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("分析失败: ${e.message ?: "未知错误"}")
        }
    }

    /**
     * 构建发送给AI的分析提示
     */
    private fun buildAnalysisPrompt(transactions: List<com.example.funnyexpensetracking.data.local.entity.TransactionEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

        // 按类别统计
        val expenseByCategory = transactions
            .filter { it.type == EntityTransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val incomeByCategory = transactions
            .filter { it.type == EntityTransactionType.INCOME }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        // 总收支
        val totalIncome = transactions
            .filter { it.type == EntityTransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = transactions
            .filter { it.type == EntityTransactionType.EXPENSE }
            .sumOf { it.amount }

        // 按月统计
        val monthlyStats = transactions.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
        }.map { (month, list) ->
            val income = list.filter { it.type == EntityTransactionType.INCOME }.sumOf { it.amount }
            val expense = list.filter { it.type == EntityTransactionType.EXPENSE }.sumOf { it.amount }
            "$month: 收入¥${"%.2f".format(income)}, 支出¥${"%.2f".format(expense)}"
        }

        return """
请分析以下用户消费数据：

【总体情况】
- 分析时间范围：最近3个月
- 总收入：¥${"%.2f".format(totalIncome)}
- 总支出：¥${"%.2f".format(totalExpense)}
- 净收入：¥${"%.2f".format(totalIncome - totalExpense)}

【月度统计】
${monthlyStats.joinToString("\n")}

【支出分类明细】
${expenseByCategory.entries.sortedByDescending { it.value }
    .joinToString("\n") { "- ${it.key}: ¥${"%.2f".format(it.value)}" }}

【收入分类明细】
${incomeByCategory.entries.sortedByDescending { it.value }
    .joinToString("\n") { "- ${it.key}: ¥${"%.2f".format(it.value)}" }}

【最近交易记录】（最新10条）
${transactions.sortedByDescending { it.date }.take(10)
    .joinToString("\n") { 
        val typeStr = if (it.type == EntityTransactionType.INCOME) "收入" else "支出"
        "- ${dateFormat.format(Date(it.date))} [$typeStr] ${it.category}: ¥${"%.2f".format(it.amount)} ${if (it.note.isNotEmpty()) "(${it.note})" else ""}"
    }}

请根据以上数据进行分析，识别消费习惯并提供建议。
"""
    }

    /**
     * 解析AI返回的JSON结果
     */
    private fun parseAIResponse(content: String): AIAnalysisResult {
        return try {
            // 尝试提取JSON（AI可能会在JSON前后添加额外文字）
            val jsonContent = extractJson(content)
            val parsed = gson.fromJson(jsonContent, ParsedAIAnalysis::class.java)

            AIAnalysisResult(
                analysisId = UUID.randomUUID().toString(),
                summary = parsed.summary,
                spendingHabits = parsed.habits.map { habit ->
                    HabitInsight(
                        category = habit.category,
                        insight = habit.insight,
                        trend = when (habit.trend.lowercase()) {
                            "increasing" -> HabitTrend.INCREASING
                            "decreasing" -> HabitTrend.DECREASING
                            else -> HabitTrend.STABLE
                        }
                    )
                },
                suggestions = parsed.suggestions.map { suggestion ->
                    Suggestion(
                        title = suggestion.title,
                        description = suggestion.description,
                        priority = when (suggestion.priority.lowercase()) {
                            "high" -> SuggestionPriority.HIGH
                            "medium" -> SuggestionPriority.MEDIUM
                            else -> SuggestionPriority.LOW
                        }
                    )
                },
                predictions = parsed.prediction?.let {
                    Prediction(
                        nextMonthExpense = it.nextMonthExpense,
                        nextMonthIncome = it.nextMonthIncome,
                        savingsPotential = it.savingsPotential
                    )
                },
                generatedAt = System.currentTimeMillis()
            )
        } catch (_: JsonSyntaxException) {
            // 如果JSON解析失败，创建一个基本的分析结果
            AIAnalysisResult(
                analysisId = UUID.randomUUID().toString(),
                summary = content.take(500), // 直接使用AI回复作为摘要
                spendingHabits = emptyList(),
                suggestions = listOf(
                    Suggestion(
                        title = "分析完成",
                        description = "AI分析已完成，请查看摘要了解详情",
                        priority = SuggestionPriority.MEDIUM
                    )
                ),
                predictions = null,
                generatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * 从文本中提取JSON
     */
    private fun extractJson(content: String): String {
        // 尝试找到JSON的开始和结束位置
        val startIndex = content.indexOf('{')
        val endIndex = content.lastIndexOf('}')

        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            content.substring(startIndex, endIndex + 1)
        } else {
            content
        }
    }

    override suspend fun getSuggestions(): Resource<List<Suggestion>> {
        // 复用analyzeHabits获取建议
        return when (val result = analyzeHabits()) {
            is Resource.Success -> Resource.Success(result.data?.suggestions ?: emptyList())
            is Resource.Error -> Resource.Error(result.message ?: "获取建议失败")
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getAnalysisHistory(limit: Int): Resource<List<AIAnalysisResult>> {
        // 本地实现暂不支持历史记录，返回空列表
        // 如需支持，可以将分析结果保存到本地数据库
        return Resource.Success(emptyList())
    }

    override suspend fun getAnalysisById(analysisId: String): Resource<AIAnalysisResult> {
        // 本地实现暂不支持按ID获取
        return Resource.Error("本地模式不支持获取历史分析记录")
    }
}

