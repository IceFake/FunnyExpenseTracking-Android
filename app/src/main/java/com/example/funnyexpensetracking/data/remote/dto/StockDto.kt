package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 股票行情DTO
 */
data class StockQuoteDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("current_price") val currentPrice: Double,
    @SerializedName("open_price") val openPrice: Double,
    @SerializedName("high_price") val highPrice: Double,
    @SerializedName("low_price") val lowPrice: Double,
    @SerializedName("close_price") val closePrice: Double,
    @SerializedName("change") val change: Double,
    @SerializedName("change_percent") val changePercent: Double,
    @SerializedName("volume") val volume: Long,
    @SerializedName("timestamp") val timestamp: Long
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

/**
 * 股票搜索结果
 */
data class StockSearchResult(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("exchange") val exchange: String? = null
)

