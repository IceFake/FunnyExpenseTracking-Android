package com.example.funnyexpensetracking.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.funnyexpensetracking.data.local.dao.InvestmentDao
import com.example.funnyexpensetracking.data.local.dao.StockHoldingDao
import com.example.funnyexpensetracking.data.remote.api.SinaFinanceApiService
import com.example.funnyexpensetracking.data.remote.dto.SinaQuoteParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 股票价格同步 Worker — 每 15 分钟通过新浪财经 API 同步最新股价
 *
 * 同步范围：
 * 1. investments 表中 category = 'STOCK' 的所有记录（description 字段存储股票代码）
 * 2. stock_holdings 表中的所有持仓记录（symbol 字段存储股票代码）
 *
 * 股票代码格式（新浪财经）：
 * - A 股上证：sh600519
 * - A 股深证：sz000001
 * - 港股：hk00700
 * - 美股：gb_aapl
 *
 * 工作流程：
 * 1. 从两张表中收集所有需要同步的股票代码（去重）
 * 2. 调用新浪财经 API 批量获取行情
 * 3. 解析响应并分别更新两张表中的 currentPrice
 */
@HiltWorker
class StockPriceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val investmentDao: InvestmentDao,
    private val stockHoldingDao: StockHoldingDao,
    private val sinaFinanceApiService: SinaFinanceApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始同步股票价格...")

            // 1. 收集所有需要同步的股票代码
            val investmentStockCodes = investmentDao.getAllStockCodes()
            val holdingSymbols = stockHoldingDao.getAllStockHoldings().first()
                .map { it.symbol }

            // 合并去重（统一转小写匹配新浪 API 格式）
            val allSymbols = (investmentStockCodes + holdingSymbols)
                .map { it.lowercase().trim() }
                .filter { it.isNotBlank() }
                .distinct()

            if (allSymbols.isEmpty()) {
                Log.d(TAG, "没有需要同步的股票，跳过")
                return Result.success()
            }

            Log.d(TAG, "待同步股票: ${allSymbols.joinToString(", ")}")

            // 2. 批量查询新浪财经 API（逗号分隔）
            val symbolsParam = allSymbols.joinToString(",")
            val response = sinaFinanceApiService.getQuotes(symbolsParam)

            if (!response.isSuccessful || response.body() == null) {
                Log.e(TAG, "API 请求失败: ${response.code()} ${response.message()}")
                return if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
            }

            // 3. 解析响应
            val quotes = SinaQuoteParser.parse(response.body()!!)

            if (quotes.isEmpty()) {
                Log.w(TAG, "未解析到有效行情数据")
                return Result.success()
            }

            Log.d(TAG, "成功获取 ${quotes.size} 只股票行情")

            // 4. 更新数据库
            var updatedCount = 0
            for (quote in quotes) {
                val symbolLower = quote.symbol.lowercase()
                val price = quote.currentPrice

                if (price <= 0) continue

                // 更新 investments 表（按 description 匹配，大小写不敏感）
                try {
                    // investments 表中 description 可能存储原始代码格式
                    investmentStockCodes.forEach { code ->
                        if (code.lowercase() == symbolLower) {
                            investmentDao.updateStockPrice(code, price)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "更新 investments 表失败: ${quote.symbol}", e)
                }

                // 更新 stock_holdings 表（按 symbol 匹配）
                try {
                    holdingSymbols.forEach { sym ->
                        if (sym.lowercase() == symbolLower) {
                            stockHoldingDao.updatePrice(sym, price)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "更新 stock_holdings 表失败: ${quote.symbol}", e)
                }

                updatedCount++
            }

            Log.d(TAG, "股票价格同步完成，成功更新 $updatedCount 只股票")

            Result.success()
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "网络不可用，稍后重试", e)
            if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "请求超时，稍后重试", e)
            if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "股票价格同步失败", e)
            if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "stock_price_sync_work"
        private const val TAG = "StockPriceSyncWorker"
        private const val MAX_RETRY_COUNT = 3
    }
}

