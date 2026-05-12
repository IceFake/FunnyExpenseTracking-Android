package com.example.funnyexpensetracking.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 股票行情 DTO（与 funny-expense-backend [StockQuoteDto] 的 JSON 字段对齐，camelCase）。
 */
data class StockQuoteDto(
    val symbol: String,
    val name: String,
    /** 当前价（后端字段名 price） */
    val price: Double? = null,
    val change: Double? = null,
    val changePercent: Double? = null,
    val volume: Long? = null,
    val marketCap: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val open: Double? = null,
    val previousClose: Double? = null,
    /** 后端为拼接的时间描述字符串 */
    val timestamp: String? = null,
    /** 兼容旧版 snake_case 假数据 / 文档示例 */
    @SerializedName("current_price") val currentPriceSnake: Double? = null,
    @SerializedName("open_price") val openPriceSnake: Double? = null,
    @SerializedName("high_price") val highPriceSnake: Double? = null,
    @SerializedName("low_price") val lowPriceSnake: Double? = null,
    @SerializedName("close_price") val closePriceSnake: Double? = null
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
