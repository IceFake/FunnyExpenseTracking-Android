package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.remote.api.StatisticsApiService
import com.example.funnyexpensetracking.data.remote.dto.StatisticsRequest
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.StatisticsRepository
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统计数据Repository实现类
 *
 * 数据获取策略：
 * - 在线 → 优先从后端API获取实时数据（含服务端图表），超时 5 秒
 * - 离线 → 直接从本地Room数据库聚合计算（离线但完整可用）
 */
@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsApiService: StatisticsApiService,
    private val transactionDao: TransactionDao
) : StatisticsRepository {

    companion object {
        /** API 超时毫秒，超时后自动切本地数据源 */
        private const val API_TIMEOUT_MS = 5000L
    }

    // ==================== 月度统计 ====================

    override suspend fun getMonthlyStatistics(year: Int, month: Int): Resource<Statistics> {
        val apiResult = withTimeoutOrNull(API_TIMEOUT_MS) {
            try {
                val response = statisticsApiService.getMonthlyStatistics(
                    StatisticsRequest(period = "monthly", year = year, month = month)
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()!!.data!!
                } else null
            } catch (_: Exception) { null }
        }
        return if (apiResult != null) {
            Resource.Success(mapMonthlyFromDto(apiResult))
        } else {
            getLocalMonthlyStatistics(year, month)
        }
    }

    // ==================== 年度统计 ====================

    override suspend fun getYearlyStatistics(year: Int): Resource<Statistics> {
        val apiResult = withTimeoutOrNull(API_TIMEOUT_MS) {
            try {
                val response = statisticsApiService.getYearlyStatistics(
                    StatisticsRequest(period = "yearly", year = year)
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()!!.data!!
                } else null
            } catch (_: Exception) { null }
        }
        return if (apiResult != null) {
            Resource.Success(mapMonthlyFromDto(apiResult))
        } else {
            getLocalYearlyStatistics(year)
        }
    }

    // ==================== 分类统计 ====================

    override suspend fun getCategoryStatistics(year: Int, month: Int): Resource<List<CategoryStat>> {
        val apiResult = withTimeoutOrNull(API_TIMEOUT_MS) {
            try {
                val response = statisticsApiService.getCategoryStatistics(year, month)
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()!!.data!!
                } else null
            } catch (_: Exception) { null }
        }
        return if (apiResult != null) {
            Resource.Success(apiResult.map { catDto ->
                CategoryStat(
                    category = catDto.category,
                    amount = catDto.amount,
                    percentage = catDto.percentage,
                    type = if (catDto.type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
                    dataSource = DataSource.REMOTE
                )
            })
        } else {
            getLocalCategoryStatistics(year, month)
        }
    }

    // ==================== 趋势统计 ====================

    override suspend fun getTrendStatistics(months: Int): Resource<TrendStatistics> {
        val apiResult = withTimeoutOrNull(API_TIMEOUT_MS) {
            try {
                val response = statisticsApiService.getTrendStatistics(months)
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()!!.data!!
                } else null
            } catch (_: Exception) { null }
        }
        return if (apiResult != null) {
            val statsList = apiResult.map { dto ->
                Statistics(
                    period = if (dto.period == "monthly") StatisticsPeriod.MONTHLY else StatisticsPeriod.YEARLY,
                    startDate = dto.startDate,
                    endDate = dto.endDate,
                    totalIncome = dto.totalIncome,
                    totalExpense = dto.totalExpense,
                    netIncome = dto.netIncome,
                    categoryBreakdown = dto.categoryBreakdown.map { catDto ->
                        CategoryStat(
                            category = catDto.category,
                            amount = catDto.amount,
                            percentage = catDto.percentage,
                            type = if (catDto.type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
                            dataSource = DataSource.REMOTE
                        )
                    },
                    chartUrl = dto.chartUrl,
                    dataSource = DataSource.REMOTE
                )
            }

            val avgIncome = statsList.map { it.totalIncome }.average()
            val avgExpense = statsList.map { it.totalExpense }.average()

            Resource.Success(
                TrendStatistics(
                    periodStats = statsList,
                    avgIncome = avgIncome,
                    avgExpense = avgExpense,
                    incomeTrend = calculateTrend(statsList.map { it.totalIncome }),
                    expenseTrend = calculateTrend(statsList.map { it.totalExpense })
                )
            )
        } else {
            getLocalTrendStatistics(months)
        }
    }

    // ==================== DTO 映射 ====================

    private fun mapMonthlyFromDto(dto: com.example.funnyexpensetracking.data.remote.dto.StatisticsDto): Statistics {
        return Statistics(
            period = StatisticsPeriod.MONTHLY,
            startDate = dto.startDate,
            endDate = dto.endDate,
            totalIncome = dto.totalIncome,
            totalExpense = dto.totalExpense,
            netIncome = dto.netIncome,
            categoryBreakdown = dto.categoryBreakdown.map { catDto ->
                CategoryStat(
                    category = catDto.category,
                    amount = catDto.amount,
                    percentage = catDto.percentage,
                    type = if (catDto.type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
                    dataSource = DataSource.REMOTE
                )
            },
            chartUrl = dto.chartUrl,
            dataSource = DataSource.REMOTE
        )
    }

    // ==================== 趋势计算 ====================

    private fun calculateTrend(values: List<Double>): TrendDirection {
        if (values.size < 2) return TrendDirection.STABLE
        val half = values.size / 2
        val first = values.take(half).average()
        val second = values.takeLast(values.size - half).average()
        val changePercent = if (first != 0.0) (second - first) / first * 100 else 0.0
        return when {
            changePercent > 5 -> TrendDirection.INCREASING
            changePercent < -5 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    // ==================== 本地数据库聚合 ====================

    /**
     * 从本地Room数据库聚合月度统计（离线路径）
     */
    private suspend fun getLocalMonthlyStatistics(year: Int, month: Int): Resource<Statistics> {
        return try {
            val (startDate, endDate) = getMonthRange(year, month)
            val transactions = queryTransactions(startDate, endDate)

            val incomeTransactions = transactions.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME
            }
            val expenseTransactions = transactions.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE
            }

            val totalIncome = incomeTransactions.sumOf { it.amount }
            val totalExpense = expenseTransactions.sumOf { it.amount }

            // 分类聚合
            val categoryBreakdown = computeCategoryBreakdown(expenseTransactions, incomeTransactions, totalExpense, totalIncome)

            // 每日趋势
            val dailyTrends = computeDailyTrends(year, month, transactions)

            Resource.Success(
                Statistics(
                    period = StatisticsPeriod.MONTHLY,
                    startDate = startDate,
                    endDate = endDate,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netIncome = totalIncome - totalExpense,
                    categoryBreakdown = categoryBreakdown,
                    dailyTrends = dailyTrends,
                    chartUrl = null,
                    dataSource = DataSource.LOCAL
                )
            )
        } catch (e: Exception) {
            Resource.Error("本地数据聚合失败: ${e.message}")
        }
    }

    /**
     * 从本地Room数据库聚合年度统计（离线路径）
     */
    private suspend fun getLocalYearlyStatistics(year: Int): Resource<Statistics> {
        return try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startDate = calendar.timeInMillis
            calendar.add(Calendar.YEAR, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endDate = calendar.timeInMillis

            val transactions = queryTransactions(startDate, endDate)
            val incomeTransactions = transactions.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME
            }
            val expenseTransactions = transactions.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE
            }

            val totalIncome = incomeTransactions.sumOf { it.amount }
            val totalExpense = expenseTransactions.sumOf { it.amount }
            val categoryBreakdown = computeCategoryBreakdown(expenseTransactions, incomeTransactions, totalExpense, totalIncome)

            Resource.Success(
                Statistics(
                    period = StatisticsPeriod.YEARLY,
                    startDate = startDate,
                    endDate = endDate,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netIncome = totalIncome - totalExpense,
                    categoryBreakdown = categoryBreakdown,
                    dailyTrends = emptyList(),
                    chartUrl = null,
                    dataSource = DataSource.LOCAL
                )
            )
        } catch (e: Exception) {
            Resource.Error("本地年度数据聚合失败: ${e.message}")
        }
    }

    /**
     * 从本地数据库聚合分类统计（离线路径）
     */
    private suspend fun getLocalCategoryStatistics(year: Int, month: Int): Resource<List<CategoryStat>> {
        return try {
            val (startDate, endDate) = getMonthRange(year, month)
            val transactions = queryTransactions(startDate, endDate)

            val incomeTransactions = transactions.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME
            }
            val expenseTransactions = transactions.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE
            }

            val totalIncome = incomeTransactions.sumOf { it.amount }
            val totalExpense = expenseTransactions.sumOf { it.amount }

            val categories = computeCategoryBreakdown(expenseTransactions, incomeTransactions, totalExpense, totalIncome)
            Resource.Success(categories)
        } catch (e: Exception) {
            Resource.Error("本地分类聚合失败: ${e.message}")
        }
    }

    /**
     * 从本地数据库聚合趋势统计（离线路径）
     */
    private suspend fun getLocalTrendStatistics(months: Int): Resource<TrendStatistics> {
        return try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val statsList = mutableListOf<Statistics>()
            for (i in (months - 1) downTo 0) {
                val monthCal = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    add(Calendar.MONTH, -i)
                }
                val year = monthCal.get(Calendar.YEAR)
                val month = monthCal.get(Calendar.MONTH) + 1

                val result = getLocalMonthlyStatistics(year, month)
                if (result is Resource.Success) {
                    statsList.add(result.data!!)
                }
            }

            if (statsList.isEmpty()) {
                return Resource.Error("暂无本地趋势数据")
            }

            val avgIncome = statsList.map { it.totalIncome }.average()
            val avgExpense = statsList.map { it.totalExpense }.average()

            Resource.Success(
                TrendStatistics(
                    periodStats = statsList,
                    avgIncome = avgIncome,
                    avgExpense = avgExpense,
                    incomeTrend = calculateTrend(statsList.map { it.totalIncome }),
                    expenseTrend = calculateTrend(statsList.map { it.totalExpense })
                )
            )
        } catch (e: Exception) {
            Resource.Error("本地趋势聚合失败: ${e.message}")
        }
    }

    // ==================== 通用工具方法 ====================

    /**
     * 获取月份的时间范围
     */
    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startDate = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis
        return Pair(startDate, endDate)
    }

    /**
     * 从Room查询交易记录（排除待删除的）
     */
    private suspend fun queryTransactions(startDate: Long, endDate: Long) =
        transactionDao.getTransactionsByDateRange(startDate, endDate)
            .first()
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE }

    /**
     * 按分类聚合收支数据
     */
    private fun computeCategoryBreakdown(
        expenseTransactions: List<com.example.funnyexpensetracking.data.local.entity.TransactionEntity>,
        incomeTransactions: List<com.example.funnyexpensetracking.data.local.entity.TransactionEntity>,
        totalExpense: Double,
        totalIncome: Double
    ): List<CategoryStat> {
        val breakdown = mutableListOf<CategoryStat>()

        // 支出分类
        expenseTransactions.groupBy { it.category }.forEach { (category, txns) ->
            val amount = txns.sumOf { it.amount }
            breakdown.add(CategoryStat(
                category = category,
                amount = amount,
                percentage = if (totalExpense > 0) amount / totalExpense * 100 else 0.0,
                type = TransactionType.EXPENSE,
                dataSource = DataSource.LOCAL
            ))
        }

        // 收入分类
        incomeTransactions.groupBy { it.category }.forEach { (category, txns) ->
            val amount = txns.sumOf { it.amount }
            breakdown.add(CategoryStat(
                category = category,
                amount = amount,
                percentage = if (totalIncome > 0) amount / totalIncome * 100 else 0.0,
                type = TransactionType.INCOME,
                dataSource = DataSource.LOCAL
            ))
        }

        return breakdown.sortedByDescending { it.amount }
    }

    /**
     * 按天拆分交易，生成每日趋势
     */
    private fun computeDailyTrends(
        year: Int, month: Int,
        transactions: List<com.example.funnyexpensetracking.data.local.entity.TransactionEntity>
    ): List<DailyTrend> {
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        val trends = mutableListOf<DailyTrend>()
        for (day in 1..daysInMonth) {
            val dayStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dayEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                add(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis

            val dayTxns = transactions.filter { it.date in dayStart..dayEnd }
            val dayIncome = dayTxns.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME
            }.sumOf { it.amount }
            val dayExpense = dayTxns.filter {
                it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE
            }.sumOf { it.amount }

            trends.add(DailyTrend(day = day, income = dayIncome, expense = dayExpense))
        }
        return trends
    }
}
