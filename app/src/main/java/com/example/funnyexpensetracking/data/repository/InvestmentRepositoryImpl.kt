package com.example.funnyexpensetracking.data.repository

import android.util.Log
import com.example.funnyexpensetracking.data.local.dao.InvestmentDao
import com.example.funnyexpensetracking.data.local.entity.InvestmentCategory
import com.example.funnyexpensetracking.data.local.entity.InvestmentEntity
import com.example.funnyexpensetracking.data.remote.api.SinaFinanceApiService
import com.example.funnyexpensetracking.data.remote.dto.SinaQuoteParser
import com.example.funnyexpensetracking.data.remote.dto.StockPriceResult
import com.example.funnyexpensetracking.domain.model.Investment
import com.example.funnyexpensetracking.domain.model.InvestmentCategory as DomainCategory
import com.example.funnyexpensetracking.domain.repository.InvestmentRepository
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 投资Repository实现类
 * 使用新浪财经 API 获取股票价格（中国大陆可访问）
 */
@Singleton
class InvestmentRepositoryImpl @Inject constructor(
    private val investmentDao: InvestmentDao,
    private val sinaFinanceApiService: SinaFinanceApiService
) : InvestmentRepository {

    companion object {
        private const val TAG = "InvestmentRepository"
    }

    override fun getAllInvestments(): Flow<List<Investment>> {
        return investmentDao.getAllInvestments().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getInvestmentById(id: Long): Investment? {
        return investmentDao.getById(id)?.toDomainModel()
    }

    override suspend fun addInvestment(investment: Investment): Long {
        return investmentDao.insert(investment.toEntity())
    }

    override suspend fun updateInvestment(investment: Investment) {
        investmentDao.update(investment.toEntity())
    }

    override suspend fun deleteInvestment(id: Long) {
        investmentDao.deleteById(id)
    }

    override suspend fun getAllStockCodes(): List<String> {
        return investmentDao.getAllStockCodes()
    }

    override suspend fun updateStockPrice(stockCode: String, price: Double) {
        investmentDao.updateStockPrice(stockCode, price)
    }

    override suspend fun refreshAllStockPrices(): Resource<Unit> {
        return try {
            val stockCodes = investmentDao.getAllStockCodes()
            Log.d(TAG, "获取到的股票代码: $stockCodes")

            if (stockCodes.isEmpty()) {
                Log.d(TAG, "没有股票需要刷新价格")
                return Resource.Success(Unit)
            }

            // 转换股票代码为新浪格式
            val sinaSymbols = stockCodes.map { convertToSinaSymbol(it) }
            val symbolsParam = sinaSymbols.joinToString(",")
            Log.d(TAG, "请求新浪财经 API, symbols: $symbolsParam")

            val response = sinaFinanceApiService.getQuotes(symbolsParam)
            Log.d(TAG, "API响应码: ${response.code()}, 是否成功: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val responseText = response.body()!!
                Log.d(TAG, "API响应内容: $responseText")

                val priceResults = SinaQuoteParser.parse(responseText)
                Log.d(TAG, "解析到 ${priceResults.size} 个股票价格")

                if (priceResults.isEmpty()) {
                    Log.w(TAG, "没有获取到有效的股票价格数据")
                    return Resource.Error("未找到股票数据，请检查股票代码格式")
                }

                // 更新每个股票的价格
                priceResults.forEach { result ->
                    Log.d(TAG, "更新股票 ${result.symbol} 价格: ${result.currentPrice}")
                    // 找到原始代码并更新
                    val originalCode = findOriginalCode(stockCodes, result.symbol)
                    if (originalCode != null) {
                        investmentDao.updateStockPrice(originalCode, result.currentPrice)
                    }
                }

                Resource.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API请求失败: ${response.code()}, 错误信息: $errorBody")
                Resource.Error("获取股票价格失败: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新股票价格异常", e)
            Resource.Error("网络错误: ${e.message}")
        }
    }

    /**
     * 将用户输入的股票代码转换为新浪格式
     *
     * 支持的输入格式：
     * - A股上证：600519 或 SH600519 或 sh600519
     * - A股深证：000001 或 SZ000001 或 sz000001
     * - 港股：00700 或 HK00700 或 hk00700
     * - 美股：AAPL 或 aapl
     */
    private fun convertToSinaSymbol(code: String): String {
        val upperCode = code.uppercase().trim()

        return when {
            // 已经是新浪格式
            upperCode.startsWith("SH") || upperCode.startsWith("SZ") -> upperCode.lowercase()
            upperCode.startsWith("HK") -> upperCode.lowercase()
            upperCode.startsWith("GB_") -> upperCode.lowercase()

            // 6位数字开头的是上证
            upperCode.matches(Regex("^6\\d{5}$")) -> "sh$upperCode"

            // 0、3开头的6位数字是深证
            upperCode.matches(Regex("^[03]\\d{5}$")) -> "sz$upperCode"

            // 5位数字港股
            upperCode.matches(Regex("^\\d{5}$")) -> "hk$upperCode"

            // 其他当作美股处理
            else -> "gb_${upperCode.lowercase()}"
        }
    }

    /**
     * 根据新浪格式的代码找到原始输入的代码
     */
    private fun findOriginalCode(originalCodes: List<String>, sinaSymbol: String): String? {
        val sinaUpper = sinaSymbol.uppercase()

        for (code in originalCodes) {
            val converted = convertToSinaSymbol(code).uppercase()
            if (converted == sinaUpper) {
                return code
            }
        }

        return null
    }

    override suspend fun getTotalStockValue(): Double {
        // 需要计算所有股票的当前价值
        var total = 0.0
        investmentDao.getAllInvestments().collect { entities ->
            total = entities
                .filter { it.category == InvestmentCategory.STOCK }
                .sumOf { entity ->
                    if (entity.currentPrice > 0) {
                        entity.currentPrice * entity.quantity
                    } else {
                        entity.investment
                    }
                }
        }
        return total
    }

    override suspend fun getTotalStockInvestment(): Double {
        var total = 0.0
        investmentDao.getAllInvestments().collect { entities ->
            total = entities
                .filter { it.category == InvestmentCategory.STOCK }
                .sumOf { it.investment }
        }
        return total
    }

    private fun InvestmentEntity.toDomainModel(): Investment {
        return Investment(
            id = id,
            category = when (category) {
                InvestmentCategory.STOCK -> DomainCategory.STOCK
                InvestmentCategory.OTHER -> DomainCategory.OTHER
            },
            description = description,
            quantity = quantity,
            investment = investment,
            currentPrice = currentPrice,
            currentValue = currentValue,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Investment.toEntity(): InvestmentEntity {
        return InvestmentEntity(
            id = id,
            category = when (category) {
                DomainCategory.STOCK -> InvestmentCategory.STOCK
                DomainCategory.OTHER -> InvestmentCategory.OTHER
            },
            description = description,
            quantity = quantity,
            investment = investment,
            currentPrice = currentPrice,
            currentValue = currentValue,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

