package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.Statistics
import com.example.funnyexpensetracking.domain.model.CategoryStat
import com.example.funnyexpensetracking.domain.model.TrendStatistics
import com.example.funnyexpensetracking.util.Resource

/**
 * 统计数据Repository接口
 */
interface StatisticsRepository {

    /**
     * 获取月度统计数据（从后端获取，包含图表）
     */
    suspend fun getMonthlyStatistics(year: Int, month: Int): Resource<Statistics>

    /**
     * 获取年度统计数据（从后端获取，包含图表）
     */
    suspend fun getYearlyStatistics(year: Int): Resource<Statistics>

    /**
     * 获取分类统计
     */
    suspend fun getCategoryStatistics(year: Int, month: Int): Resource<List<CategoryStat>>

    /**
     * 获取趋势统计（最近N个月）
     */
    suspend fun getTrendStatistics(months: Int = 6): Resource<TrendStatistics>
}

