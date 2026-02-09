package com.example.funnyexpensetracking.data.remote.dto

import android.util.Log
import com.google.gson.annotations.SerializedName

/**
 * 新浪财经 API 响应解析工具
 *
 * 响应格式示例：
 * A股：var hq_str_sh600519="贵州茅台,1800.00,1795.00,1810.00,1815.00,1790.00,1809.00,1810.00,12345678,22000000000.00,..."
 * 港股：var hq_str_hk00700="腾讯控股,TENCENT,350.000,340.000,355.000,338.000,345.000,..."
 * 美股：var hq_str_gb_aapl="苹果,180.5000,1.2000,0.67,..."
 */
object SinaQuoteParser {

    private const val TAG = "SinaQuoteParser"

    /**
     * 解析新浪财经响应文本
     */
    fun parse(responseText: String): List<StockPriceResult> {
        val results = mutableListOf<StockPriceResult>()

        // 每行一个股票
        val lines = responseText.split(";").filter { it.contains("var hq_str_") }

        for (line in lines) {
            try {
                val result = parseLine(line.trim())
                if (result != null) {
                    results.add(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析行失败: $line", e)
            }
        }

        return results
    }

    private fun parseLine(line: String): StockPriceResult? {
        // 格式：var hq_str_sh600519="贵州茅台,1800.00,..."
        val regex = """var hq_str_(\w+)="(.*)"""".toRegex()
        val matchResult = regex.find(line) ?: return null

        val symbol = matchResult.groupValues[1]
        val dataStr = matchResult.groupValues[2]

        if (dataStr.isEmpty()) {
            Log.w(TAG, "股票 $symbol 数据为空")
            return null
        }

        val parts = dataStr.split(",")
        if (parts.size < 4) {
            Log.w(TAG, "股票 $symbol 数据格式不正确: $dataStr")
            return null
        }

        return when {
            // A股（上证 sh 或深证 sz）
            symbol.startsWith("sh") || symbol.startsWith("sz") -> parseAStock(symbol, parts)
            // 港股（hk）
            symbol.startsWith("hk") -> parseHKStock(symbol, parts)
            // 美股（gb_）
            symbol.startsWith("gb_") -> parseUSStock(symbol, parts)
            else -> {
                Log.w(TAG, "未知的股票类型: $symbol")
                null
            }
        }
    }

    /**
     * 解析A股数据
     * 格式：名称,今开,昨收,现价,最高,最低,买1价,卖1价,成交量,成交额,...
     */
    private fun parseAStock(symbol: String, parts: List<String>): StockPriceResult? {
        if (parts.size < 9) return null

        val name = parts[0]
        val currentPrice = parts[3].toDoubleOrNull() ?: return null
        val previousClose = parts[2].toDoubleOrNull() ?: currentPrice

        if (currentPrice <= 0) return null

        val change = currentPrice - previousClose
        val changePercent = if (previousClose > 0) (change / previousClose) * 100 else 0.0

        return StockPriceResult(
            symbol = symbol.uppercase(),
            name = name,
            currentPrice = currentPrice,
            previousClose = previousClose,
            change = change,
            changePercent = changePercent
        )
    }

    /**
     * 解析港股数据
     * 格式：中文名,英文名,现价,涨跌额,涨跌幅,昨收,今开,最高,最低,...
     */
    private fun parseHKStock(symbol: String, parts: List<String>): StockPriceResult? {
        if (parts.size < 6) return null

        val name = parts[0]
        val currentPrice = parts[2].toDoubleOrNull() ?: return null
        val previousClose = parts[5].toDoubleOrNull() ?: currentPrice

        if (currentPrice <= 0) return null

        val change = parts[3].toDoubleOrNull() ?: (currentPrice - previousClose)
        val changePercent = parts[4].toDoubleOrNull() ?: run {
            if (previousClose > 0) (change / previousClose) * 100 else 0.0
        }

        return StockPriceResult(
            symbol = symbol.uppercase(),
            name = name,
            currentPrice = currentPrice,
            previousClose = previousClose,
            change = change,
            changePercent = changePercent
        )
    }

    /**
     * 解析美股数据
     * 格式：中文名,现价,涨跌额,涨跌幅,...
     */
    private fun parseUSStock(symbol: String, parts: List<String>): StockPriceResult? {
        if (parts.size < 4) return null

        val name = parts[0]
        val currentPrice = parts[1].toDoubleOrNull() ?: return null

        if (currentPrice <= 0) return null

        val change = parts[2].toDoubleOrNull() ?: 0.0
        val changePercent = parts[3].toDoubleOrNull() ?: 0.0
        val previousClose = currentPrice - change

        return StockPriceResult(
            symbol = symbol.uppercase(),
            name = name,
            currentPrice = currentPrice,
            previousClose = previousClose,
            change = change,
            changePercent = changePercent
        )
    }
}

/**
 * Yahoo Finance v7 Quote API 响应（保留用于兼容）
 */
data class YahooQuoteResponse(
    @SerializedName("quoteResponse") val quoteResponse: QuoteResponse?
)

data class QuoteResponse(
    @SerializedName("result") val result: List<YahooQuote>?,
    @SerializedName("error") val error: YahooApiError?
)

data class YahooQuote(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("longName") val longName: String?,
    @SerializedName("regularMarketPrice") val regularMarketPrice: Double?,
    @SerializedName("regularMarketChange") val regularMarketChange: Double?,
    @SerializedName("regularMarketChangePercent") val regularMarketChangePercent: Double?,
    @SerializedName("regularMarketPreviousClose") val regularMarketPreviousClose: Double?,
    @SerializedName("regularMarketOpen") val regularMarketOpen: Double?,
    @SerializedName("regularMarketDayHigh") val regularMarketDayHigh: Double?,
    @SerializedName("regularMarketDayLow") val regularMarketDayLow: Double?,
    @SerializedName("regularMarketVolume") val regularMarketVolume: Long?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("exchange") val exchange: String?,
    @SerializedName("marketState") val marketState: String?
)

data class YahooApiError(
    @SerializedName("code") val code: String?,
    @SerializedName("description") val description: String?
)

/**
 * 简化的股票价格结果
 */
data class StockPriceResult(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val previousClose: Double,
    val change: Double,
    val changePercent: Double
) {
    companion object {
        /**
         * 从Yahoo API响应解析股票价格（保留用于兼容）
         */
        fun fromYahooResponse(yahooResponse: YahooQuoteResponse): List<StockPriceResult> {
            val results = mutableListOf<StockPriceResult>()

            yahooResponse.quoteResponse?.result?.forEach { quote ->
                val currentPrice = quote.regularMarketPrice
                if (currentPrice != null && currentPrice > 0) {
                    val previousClose = quote.regularMarketPreviousClose ?: currentPrice
                    val change = quote.regularMarketChange ?: (currentPrice - previousClose)
                    val changePercent = quote.regularMarketChangePercent ?: run {
                        if (previousClose != 0.0) (change / previousClose) * 100 else 0.0
                    }

                    results.add(
                        StockPriceResult(
                            symbol = quote.symbol,
                            name = quote.shortName ?: quote.longName ?: quote.symbol,
                            currentPrice = currentPrice,
                            previousClose = previousClose,
                            change = change,
                            changePercent = changePercent
                        )
                    )
                }
            }

            return results
        }
    }
}

