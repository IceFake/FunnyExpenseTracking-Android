package com.example.funnyexpensetracking.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 股票行情 API 服务
 * 使用新浪财经 API 获取实时股票价格（中国大陆可访问）
 *
 * 股票代码格式：
 * - A股上证：sh600519（茅台）
 * - A股深证：sz000001（平安银行）
 * - 港股：hk00700（腾讯）
 * - 美股：gb_aapl（苹果）
 */
interface SinaFinanceApiService {

    /**
     * 获取股票行情（新浪财经）
     * 响应格式为文本，需要手动解析
     * @param symbols 股票代码列表，逗号分隔
     *
     * 示例：list=sh600519,sz000001,hk00700,gb_aapl
     */
    @GET("list")
    suspend fun getQuotes(
        @Query("list") symbols: String
    ): Response<String>
}

/**
 * 保留 YahooFinanceApiService 接口用于兼容，但实际使用新浪 API
 */
interface YahooFinanceApiService {
    @GET("v7/finance/quote")
    suspend fun getQuotes(
        @Query("symbols") symbols: String
    ): Response<com.example.funnyexpensetracking.data.remote.dto.YahooQuoteResponse>

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "1m"
    ): Response<YahooChartResponse>
}

/**
 * Yahoo Chart API响应（保留用于兼容）
 */
data class YahooChartResponse(
    val chart: ChartResult?
)

data class ChartResult(
    val result: List<ChartData>?,
    val error: YahooError?
)

data class ChartData(
    val meta: ChartMeta?,
    val timestamp: List<Long>?,
    val indicators: Indicators?
)

data class ChartMeta(
    val currency: String?,
    val symbol: String?,
    val exchangeName: String?,
    val regularMarketPrice: Double?,
    val previousClose: Double?
)

data class Indicators(
    val quote: List<QuoteIndicator>?
)

data class QuoteIndicator(
    val open: List<Double?>?,
    val high: List<Double?>?,
    val low: List<Double?>?,
    val close: List<Double?>?,
    val volume: List<Long?>?
)

data class YahooError(
    val code: String?,
    val description: String?
)

