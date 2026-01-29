package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.remote.api.StatisticsApiService
import com.example.funnyexpensetracking.data.remote.dto.StatisticsRequest
import com.example.funnyexpensetracking.domain.model.*
import com.example.funnyexpensetracking.domain.repository.StatisticsRepository
import com.example.funnyexpensetracking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统计数据Repository实现类
 */
@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsApiService: StatisticsApiService
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
                Resource.Error(response.message() ?: "获取月度统计失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
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
                Resource.Error(response.message() ?: "获取年度统计失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
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
        val changePercent = (second - first) / first * 100
        return when {
            changePercent > 5 -> TrendDirection.INCREASING
            changePercent < -5 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
}

