package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.remote.api.StatisticsApiService
import com.example.funnyexpensetracking.data.remote.dto.StatisticsRequest
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.StatisticsRepository
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统计数据Repository实现类
 */
@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsApiService: StatisticsApiService,
    private val transactionDao: TransactionDao
) : StatisticsRepository {

    override suspend fun getMonthlyStatistics(year: Int, month: Int): Resource<Statistics> {
        return try {
            val response = statisticsApiService.getMonthlyStatistics(
                StatisticsRequest(period = "monthly", year = year, month = month)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                Resource.Success(
                    Statistics(
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
                                type = if (catDto.type == "income") TransactionType.INCOME else TransactionType.EXPENSE
                            )
                        },
                        chartUrl = dto.chartUrl
                    )
                )
            } else {
                // API 失败，使用本地数据
                getLocalMonthlyStatistics(year, month)
            }
        } catch (e: Exception) {
            // 网络错误，使用本地数据
            getLocalMonthlyStatistics(year, month)
        }
    }

    override suspend fun getYearlyStatistics(year: Int): Resource<Statistics> {
        return try {
            val response = statisticsApiService.getYearlyStatistics(
                StatisticsRequest(period = "yearly", year = year)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                Resource.Success(
                    Statistics(
                        period = StatisticsPeriod.YEARLY,
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
                                type = if (catDto.type == "income") TransactionType.INCOME else TransactionType.EXPENSE
                            )
                        },
                        chartUrl = dto.chartUrl
                    )
                )
            } else {
                // API 失败，使用本地数据
                getLocalYearlyStatistics(year)
            }
        } catch (e: Exception) {
            // 网络错误，使用本地数据
            getLocalYearlyStatistics(year)
        }
    }

    override suspend fun getCategoryStatistics(year: Int, month: Int): Resource<List<CategoryStat>> {
        return try {
            val response = statisticsApiService.getCategoryStatistics(year, month)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(
                    response.body()!!.data!!.map { catDto ->
                        CategoryStat(
                            category = catDto.category,
                            amount = catDto.amount,
                            percentage = catDto.percentage,
                            type = if (catDto.type == "income") TransactionType.INCOME else TransactionType.EXPENSE
                        )
                    }
                )
            } else {
                Resource.Error(response.message() ?: "获取分类统计失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    override suspend fun getTrendStatistics(months: Int): Resource<TrendStatistics> {
        return try {
            val response = statisticsApiService.getTrendStatistics(months)
            if (response.isSuccessful && response.body()?.data != null) {
                val statsList = response.body()!!.data!!.map { dto ->
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
                                type = if (catDto.type == "income") TransactionType.INCOME else TransactionType.EXPENSE
                            )
                        },
                        chartUrl = dto.chartUrl
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
                Resource.Error(response.message() ?: "获取趋势统计失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    private fun calculateTrend(values: List<Double>): TrendDirection {
        if (values.size < 2) return TrendDirection.STABLE
        val first = values.take(values.size / 2).average()
        val second = values.takeLast(values.size / 2).average()
        val changePercent = if (first != 0.0) (second - first) / first * 100 else 0.0
        return when {
            changePercent > 5 -> TrendDirection.INCREASING
            changePercent < -5 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    /**
     * 从本地数据库生成月度统计
     */
    private suspend fun getLocalMonthlyStatistics(year: Int, month: Int): Resource<Statistics> {
        return try {
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

            val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate)
                .first()
                .filter { it.syncStatus != SyncStatus.PENDING_DELETE }

            val incomeTransactions = transactions.filter { it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME }
            val expenseTransactions = transactions.filter { it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE }

            val totalIncome = incomeTransactions.sumOf { it.amount }
            val totalExpense = expenseTransactions.sumOf { it.amount }

            // 按分类分组计算
            val categoryBreakdown = mutableListOf<CategoryStat>()

            // 支出分类统计
            val expenseByCategory = expenseTransactions.groupBy { it.category }
            expenseByCategory.forEach { (category, txns) ->
                val amount = txns.sumOf { it.amount }
                val percentage = if (totalExpense > 0) amount / totalExpense * 100 else 0.0
                categoryBreakdown.add(
                    CategoryStat(
                        category = category,
                        amount = amount,
                        percentage = percentage,
                        type = TransactionType.EXPENSE
                    )
                )
            }

            // 收入分类统计
            val incomeByCategory = incomeTransactions.groupBy { it.category }
            incomeByCategory.forEach { (category, txns) ->
                val amount = txns.sumOf { it.amount }
                val percentage = if (totalIncome > 0) amount / totalIncome * 100 else 0.0
                categoryBreakdown.add(
                    CategoryStat(
                        category = category,
                        amount = amount,
                        percentage = percentage,
                        type = TransactionType.INCOME
                    )
                )
            }

            // 按金额排序
            val sortedBreakdown = categoryBreakdown.sortedByDescending { it.amount }

            Resource.Success(
                Statistics(
                    period = StatisticsPeriod.MONTHLY,
                    startDate = startDate,
                    endDate = endDate,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netIncome = totalIncome - totalExpense,
                    categoryBreakdown = sortedBreakdown,
                    chartUrl = null
                )
            )
        } catch (e: Exception) {
            Resource.Error("获取本地统计数据失败: ${e.message}")
        }
    }

    /**
     * 从本地数据库生成年度统计
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

            val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate)
                .first()
                .filter { it.syncStatus != SyncStatus.PENDING_DELETE }

            val incomeTransactions = transactions.filter { it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.INCOME }
            val expenseTransactions = transactions.filter { it.type == com.example.funnyexpensetracking.data.local.entity.TransactionType.EXPENSE }

            val totalIncome = incomeTransactions.sumOf { it.amount }
            val totalExpense = expenseTransactions.sumOf { it.amount }

            // 按分类分组计算
            val categoryBreakdown = mutableListOf<CategoryStat>()

            // 支出分类统计
            val expenseByCategory = expenseTransactions.groupBy { it.category }
            expenseByCategory.forEach { (category, txns) ->
                val amount = txns.sumOf { it.amount }
                val percentage = if (totalExpense > 0) amount / totalExpense * 100 else 0.0
                categoryBreakdown.add(
                    CategoryStat(
                        category = category,
                        amount = amount,
                        percentage = percentage,
                        type = TransactionType.EXPENSE
                    )
                )
            }

            // 收入分类统计
            val incomeByCategory = incomeTransactions.groupBy { it.category }
            incomeByCategory.forEach { (category, txns) ->
                val amount = txns.sumOf { it.amount }
                val percentage = if (totalIncome > 0) amount / totalIncome * 100 else 0.0
                categoryBreakdown.add(
                    CategoryStat(
                        category = category,
                        amount = amount,
                        percentage = percentage,
                        type = TransactionType.INCOME
                    )
                )
            }

            // 按金额排序
            val sortedBreakdown = categoryBreakdown.sortedByDescending { it.amount }

            Resource.Success(
                Statistics(
                    period = StatisticsPeriod.YEARLY,
                    startDate = startDate,
                    endDate = endDate,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netIncome = totalIncome - totalExpense,
                    categoryBreakdown = sortedBreakdown,
                    chartUrl = null
                )
            )
        } catch (e: Exception) {
            Resource.Error("获取本地统计数据失败: ${e.message}")
        }
    }
}

