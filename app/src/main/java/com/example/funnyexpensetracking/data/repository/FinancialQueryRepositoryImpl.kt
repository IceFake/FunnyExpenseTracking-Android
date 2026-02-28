package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.UserPreferencesManager
import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.dao.FixedIncomeDao
import com.example.funnyexpensetracking.data.local.dao.InvestmentDao
import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType
import com.example.funnyexpensetracking.data.local.entity.InvestmentCategory
import com.example.funnyexpensetracking.data.remote.api.DeepSeekApiService
import com.example.funnyexpensetracking.data.remote.dto.OpenAIChatMessage
import com.example.funnyexpensetracking.data.remote.dto.OpenAIChatRequest
import com.example.funnyexpensetracking.data.remote.dto.OpenAIChatResponse
import com.example.funnyexpensetracking.domain.repository.FinancialQueryRepository
import com.example.funnyexpensetracking.util.Resource
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.example.funnyexpensetracking.data.local.entity.TransactionType as EntityTransactionType

/**
 * 自然语言财务查询Repository实现
 * 收集用户财务数据快照，构建上下文，调用DeepSeek API进行自然语言问答
 */
@Singleton
class FinancialQueryRepositoryImpl @Inject constructor(
    private val deepSeekApiService: DeepSeekApiService,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val fixedIncomeDao: FixedIncomeDao,
    private val investmentDao: InvestmentDao,
    private val userPreferencesManager: UserPreferencesManager,
    private val gson: Gson
) : FinancialQueryRepository {

    companion object {
        const val ERROR_API_KEY_MISSING = "API_KEY_MISSING"
        const val ERROR_API_KEY_INVALID = "API_KEY_INVALID"

        private const val SYSTEM_PROMPT = """
你是一个智能个人财务助手，名叫「趣味记账AI」。用户会用自然语言向你提问关于他们的财务状况。

你的职责：
1. 根据提供的财务数据，准确回答用户关于收支、账户、投资、固定收支等方面的问题
2. 回答要简洁明了，使用中文，适当使用emoji让回答更生动
3. 如果数据中没有相关信息，诚实说明并给出建议
4. 可以根据数据进行简单的分析和建议
5. 金额显示使用人民币符号¥，保留两位小数
6. 回答保持友好、专业的语气

注意事项：
- 只回答与个人财务相关的问题
- 不要编造不存在的数据
- 如果用户问的问题超出财务范围，礼貌地引导回财务话题
- 回复使用纯文本格式，不要使用Markdown标记
"""
    }

    override suspend fun queryFinancial(
        userQuestion: String,
        conversationHistory: List<OpenAIChatMessage>
    ): Resource<String> {
        return try {
            // 检查API Key
            val apiKey = userPreferencesManager.getDeepSeekApiKey()
            if (apiKey.isBlank()) {
                return Resource.Error(ERROR_API_KEY_MISSING)
            }

            // 构建财务数据上下文
            val financialContext = buildFinancialContext()

            // 构建消息列表
            val messages = mutableListOf<OpenAIChatMessage>()

            // 系统提示
            messages.add(OpenAIChatMessage(role = "system", content = SYSTEM_PROMPT))

            // 添加财务数据上下文作为系统消息
            messages.add(OpenAIChatMessage(
                role = "system",
                content = "以下是用户当前的财务数据快照，请基于这些数据回答用户的问题：\n\n$financialContext"
            ))

            // 添加对话历史（最多保留最近5轮对话）
            val recentHistory = conversationHistory.takeLast(10) // 5轮 = 10条消息
            messages.addAll(recentHistory)

            // 添加用户当前问题
            messages.add(OpenAIChatMessage(role = "user", content = userQuestion))

            // 调用DeepSeek API（不使用JSON模式，直接返回自然语言）
            val request = OpenAIChatRequest(
                model = "deepseek-chat",
                messages = messages,
                temperature = 0.7,
                maxTokens = 1500,
                responseFormat = null
            )

            val response = deepSeekApiService.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!

                if (responseBody.error != null) {
                    return Resource.Error("AI服务错误: ${responseBody.error.message}")
                }

                val aiContent = responseBody.choices?.firstOrNull()?.message?.content
                    ?: return Resource.Error("AI未返回有效回复")

                Resource.Success(aiContent.trim())
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = when (response.code()) {
                    401 -> ERROR_API_KEY_INVALID
                    402 -> "DeepSeek账户余额不足，请充值后重试"
                    429 -> "请求频率超限，请稍后重试"
                    500 -> "DeepSeek服务端错误，请稍后重试"
                    503 -> "DeepSeek服务繁忙，请稍后重试"
                    else -> "API请求失败(${response.code()}): ${parseErrorMessage(errorBody) ?: response.message()}"
                }
                Resource.Error(errorMsg)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Resource.Error("请求超时，请稍后重试")
        } catch (e: java.net.UnknownHostException) {
            Resource.Error("网络连接失败，请检查网络设置")
        } catch (e: java.io.IOException) {
            Resource.Error("网络异常: ${e.message ?: "连接中断"}")
        } catch (e: Exception) {
            Resource.Error("查询失败: ${e.message ?: "未知错误"}")
        }
    }

    /**
     * 构建财务数据上下文快照
     * 包含账户余额、本月收支、分类明细、固定收支、投资情况等
     */
    private suspend fun buildFinancialContext(): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val now = Calendar.getInstance()
        val today = dateFormat.format(Date())

        sb.appendLine("【数据查询时间】$today")
        sb.appendLine()

        // 1. 账户信息
        try {
            val accounts = accountDao.getAllAccounts().first()
            if (accounts.isNotEmpty()) {
                sb.appendLine("【账户信息】")
                var totalBalance = 0.0
                accounts.forEach { account ->
                    val defaultTag = if (account.isDefault) "（默认）" else ""
                    sb.appendLine("- ${account.name}$defaultTag: ¥${"%.2f".format(account.balance)}")
                    totalBalance += account.balance
                }
                sb.appendLine("- 账户余额合计: ¥${"%.2f".format(totalBalance)}")
                sb.appendLine()
            }
        } catch (_: Exception) { }

        // 2. 本月收支概况
        try {
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val monthEnd = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val monthIncome = transactionDao.getTotalByTypeAndDateRange(EntityTransactionType.INCOME, monthStart, monthEnd) ?: 0.0
            val monthExpense = transactionDao.getTotalByTypeAndDateRange(EntityTransactionType.EXPENSE, monthStart, monthEnd) ?: 0.0

            sb.appendLine("【本月收支概况】（${now.get(Calendar.YEAR)}年${now.get(Calendar.MONTH) + 1}月）")
            sb.appendLine("- 本月收入: ¥${"%.2f".format(monthIncome)}")
            sb.appendLine("- 本月支出: ¥${"%.2f".format(monthExpense)}")
            sb.appendLine("- 本月结余: ¥${"%.2f".format(monthIncome - monthExpense)}")
            sb.appendLine()

            // 本月分类明细
            val monthTransactions = transactionDao.getTransactionsByDateRange(monthStart, monthEnd).first()
            if (monthTransactions.isNotEmpty()) {
                val expenseByCategory = monthTransactions
                    .filter { it.type == EntityTransactionType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }

                if (expenseByCategory.isNotEmpty()) {
                    sb.appendLine("【本月支出分类明细】")
                    expenseByCategory.forEach { (category, amount) ->
                        sb.appendLine("- $category: ¥${"%.2f".format(amount)}")
                    }
                    sb.appendLine()
                }

                val incomeByCategory = monthTransactions
                    .filter { it.type == EntityTransactionType.INCOME }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }

                if (incomeByCategory.isNotEmpty()) {
                    sb.appendLine("【本月收入分类明细】")
                    incomeByCategory.forEach { (category, amount) ->
                        sb.appendLine("- $category: ¥${"%.2f".format(amount)}")
                    }
                    sb.appendLine()
                }
            }
        } catch (_: Exception) { }

        // 3. 上月收支概况
        try {
            val lastMonth = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }
            val lastMonthStart = Calendar.getInstance().apply {
                time = lastMonth.time
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val lastMonthEnd = Calendar.getInstance().apply {
                time = lastMonth.time
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val lastMonthIncome = transactionDao.getTotalByTypeAndDateRange(EntityTransactionType.INCOME, lastMonthStart, lastMonthEnd) ?: 0.0
            val lastMonthExpense = transactionDao.getTotalByTypeAndDateRange(EntityTransactionType.EXPENSE, lastMonthStart, lastMonthEnd) ?: 0.0

            sb.appendLine("【上月收支概况】（${lastMonth.get(Calendar.YEAR)}年${lastMonth.get(Calendar.MONTH) + 1}月）")
            sb.appendLine("- 上月收入: ¥${"%.2f".format(lastMonthIncome)}")
            sb.appendLine("- 上月支出: ¥${"%.2f".format(lastMonthExpense)}")
            sb.appendLine("- 上月结余: ¥${"%.2f".format(lastMonthIncome - lastMonthExpense)}")
            sb.appendLine()
        } catch (_: Exception) { }

        // 4. 最近交易记录
        try {
            val allTransactions = transactionDao.getAllTransactions().first()
            val recentTransactions = allTransactions.sortedByDescending { it.date }.take(15)
            if (recentTransactions.isNotEmpty()) {
                sb.appendLine("【最近交易记录】（最新15条）")
                recentTransactions.forEach { t ->
                    val typeStr = if (t.type == EntityTransactionType.INCOME) "收入" else "支出"
                    val note = if (t.note.isNotEmpty()) "（${t.note}）" else ""
                    sb.appendLine("- ${dateFormat.format(Date(t.date))} [$typeStr] ${t.category}: ¥${"%.2f".format(t.amount)}$note")
                }
                sb.appendLine()
            }
        } catch (_: Exception) { }

        // 5. 固定收支
        try {
            val fixedIncomes = fixedIncomeDao.getAllFixedIncomes().first()
            if (fixedIncomes.isNotEmpty()) {
                sb.appendLine("【固定收支项目】")
                fixedIncomes.forEach { fi ->
                    val typeStr = if (fi.type == FixedIncomeType.INCOME) "收入" else "支出"
                    val freqStr = when (fi.frequency) {
                        com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.DAILY -> "每日"
                        com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.WEEKLY -> "每周"
                        com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.MONTHLY -> "每月"
                        com.example.funnyexpensetracking.data.local.entity.FixedIncomeFrequency.YEARLY -> "每年"
                    }
                    val statusStr = if (fi.isActive) "生效中" else "已停用"
                    sb.appendLine("- ${fi.name}（$typeStr/$freqStr/$statusStr）: ¥${"%.2f".format(fi.amount)}/周期, 累计: ¥${"%.2f".format(fi.accumulatedAmount)}")
                }
                sb.appendLine()
            }
        } catch (_: Exception) { }

        // 6. 投资情况
        try {
            val investments = investmentDao.getAllInvestments().first()
            if (investments.isNotEmpty()) {
                sb.appendLine("【投资持仓】")
                var totalInvestment = 0.0
                var totalCurrentValue = 0.0
                investments.forEach { inv ->
                    val categoryStr = when (inv.category) {
                        InvestmentCategory.STOCK -> "股票"
                        InvestmentCategory.OTHER -> "其他"
                    }
                    val currentVal = inv.calcCurrentValue()
                    val profitLoss = inv.getProfitLoss()
                    val profitStr = if (profitLoss >= 0) "+¥${"%.2f".format(profitLoss)}" else "-¥${"%.2f".format(-profitLoss)}"
                    sb.appendLine("- ${inv.description}（$categoryStr）: 投入¥${"%.2f".format(inv.investment)}, 当前¥${"%.2f".format(currentVal)}, 盈亏$profitStr")
                    totalInvestment += inv.investment
                    totalCurrentValue += currentVal
                }
                val totalProfit = totalCurrentValue - totalInvestment
                val totalProfitStr = if (totalProfit >= 0) "+¥${"%.2f".format(totalProfit)}" else "-¥${"%.2f".format(-totalProfit)}"
                sb.appendLine("- 投资合计: 投入¥${"%.2f".format(totalInvestment)}, 当前¥${"%.2f".format(totalCurrentValue)}, 总盈亏$totalProfitStr")
                sb.appendLine()
            }
        } catch (_: Exception) { }

        // 7. 年度概况
        try {
            val yearStart = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val yearEnd = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val yearIncome = transactionDao.getTotalByTypeAndDateRange(EntityTransactionType.INCOME, yearStart, yearEnd) ?: 0.0
            val yearExpense = transactionDao.getTotalByTypeAndDateRange(EntityTransactionType.EXPENSE, yearStart, yearEnd) ?: 0.0

            sb.appendLine("【${now.get(Calendar.YEAR)}年度概况】")
            sb.appendLine("- 年度总收入: ¥${"%.2f".format(yearIncome)}")
            sb.appendLine("- 年度总支出: ¥${"%.2f".format(yearExpense)}")
            sb.appendLine("- 年度结余: ¥${"%.2f".format(yearIncome - yearExpense)}")
        } catch (_: Exception) { }

        return sb.toString()
    }

    /**
     * 解析HTTP错误响应体中的错误消息
     */
    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val errorResponse = gson.fromJson(errorBody, OpenAIChatResponse::class.java)
            errorResponse.error?.message
        } catch (_: Exception) {
            errorBody.take(200)
        }
    }
}

