package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 统计请求
 */
data class StatisticsRequest(
    @SerializedName("period") val period: String,
    @SerializedName("year") val year: Int,
    @SerializedName("month") val month: Int? = null
)

/**
 * 统计数据DTO
 */
data class StatisticsDto(
    @SerializedName("period") val period: String,
    @SerializedName("start_date") val startDate: Long,
    @SerializedName("end_date") val endDate: Long,
    @SerializedName("total_income") val totalIncome: Double,
    @SerializedName("total_expense") val totalExpense: Double,
    @SerializedName("net_income") val netIncome: Double,
    @SerializedName("category_breakdown") val categoryBreakdown: List<CategoryStatDto>,
    @SerializedName("chart_url") val chartUrl: String? = null
)

/**
 * 分类统计DTO
 */
data class CategoryStatDto(
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("percentage") val percentage: Double,
    @SerializedName("type") val type: String
)

