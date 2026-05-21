package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 股票行情 DTO（与 funny-expense-backend [StockQuoteDto] 的 JSON 字段对齐，snake_case）。
 */
data class StockQuoteDto(
    val symbol: String,
    val name: String,
    @SerializedName("current_price") val currentPrice: Double? = null,
    @SerializedName("open_price") val openPrice: Double? = null,
    @SerializedName("high_price") val highPrice: Double? = null,
    @SerializedName("low_price") val lowPrice: Double? = null,
    @SerializedName("close_price") val closePrice: Double? = null,
    val change: Double? = null,
    @SerializedName("change_percent") val changePercent: Double? = null,
    val volume: Long? = null,
    val timestamp: Long? = null
)

/**
 * 批量行情请求
 */
data class BatchQuoteRequest(
    @SerializedName("symbols") val symbols: List<String>
)

/**
 * 批量行情响应
 */
data class BatchQuoteResponse(
    @SerializedName("quotes") val quotes: List<StockQuoteDto>
)

