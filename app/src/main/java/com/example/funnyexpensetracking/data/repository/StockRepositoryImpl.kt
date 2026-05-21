package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.StockHoldingDao
import com.example.funnyexpensetracking.data.local.entity.StockHoldingEntity
import com.example.funnyexpensetracking.data.remote.api.StockApiService
import com.example.funnyexpensetracking.data.remote.dto.BatchQuoteRequest
import com.example.funnyexpensetracking.data.remote.dto.StockQuoteDto
import com.example.funnyexpensetracking.domain.model.StockHolding
import com.example.funnyexpensetracking.domain.model.StockQuote
import com.example.funnyexpensetracking.domain.repository.StockRepository
import com.example.funnyexpensetracking.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 股票Repository实现类
 */
@Singleton
class StockRepositoryImpl @Inject constructor(
    private val stockHoldingDao: StockHoldingDao,
    private val stockApiService: StockApiService
) : StockRepository {

    override fun getAllStockHoldings(): Flow<List<StockHolding>> {
        return stockHoldingDao.getAllStockHoldings().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getStockHoldingById(id: Long): StockHolding? {
        return stockHoldingDao.getById(id)?.toDomainModel()
    }

    override suspend fun getStockHoldingBySymbol(symbol: String): StockHolding? {
        return stockHoldingDao.getBySymbol(symbol)?.toDomainModel()
    }

    override suspend fun addStockHolding(stock: StockHolding): Long {
        return stockHoldingDao.insert(stock.toEntity())
    }

    override suspend fun updateStockHolding(stock: StockHolding) {
        stockHoldingDao.update(stock.toEntity())
    }

    override suspend fun deleteStockHolding(stock: StockHolding) {
        stockHoldingDao.delete(stock.toEntity())
    }

    override suspend fun getTotalStockValue(): Double {
        return stockHoldingDao.getTotalStockValue() ?: 0.0
    }

    override suspend fun getTotalStockCost(): Double {
        return stockHoldingDao.getTotalStockCost() ?: 0.0
    }

    override suspend fun getBatchQuotes(symbols: List<String>): Resource<List<StockQuote>> {
        return try {
            val response = stockApiService.getBatchQuotes(BatchQuoteRequest(symbols))
            if (response.isSuccessful && response.body()?.data != null) {
                val quotes = response.body()!!.data!!.quotes.map { it.toDomainModel() }
                quotes.forEach { quote ->
                    stockHoldingDao.updatePrice(quote.symbol, quote.currentPrice)
                }
                Resource.Success(quotes)
            } else {
                Resource.Error(response.message() ?: "批量获取行情失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    override suspend fun refreshAllStockPrices(): Resource<Unit> {
        return try {
            val holdings = stockHoldingDao.getAllStockHoldings().first()
            if (holdings.isEmpty()) return Resource.Success(Unit)

            val symbols = holdings.map { it.symbol }
            when (val result = getBatchQuotes(symbols)) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Error(result.message ?: "刷新股票价格失败")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "刷新股票价格失败")
        }
    }

    private fun StockHoldingEntity.toDomainModel(): StockHolding {
        return StockHolding(
            id = id, symbol = symbol, name = name,
            shares = shares, purchasePrice = purchasePrice,
            totalCost = totalCost, purchaseDate = purchaseDate,
            currentPrice = currentPrice, lastUpdated = lastUpdated
        )
    }

    private fun StockHolding.toEntity(): StockHoldingEntity {
        return StockHoldingEntity(
            id = id, symbol = symbol, name = name,
            shares = shares, purchasePrice = purchasePrice,
            totalCost = totalCost, purchaseDate = purchaseDate,
            currentPrice = currentPrice, lastUpdated = lastUpdated,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun StockQuoteDto.toDomainModel(): StockQuote {
        val cp = currentPrice ?: 0.0
        val op = openPrice ?: cp
        val hi = highPrice ?: cp
        val lo = lowPrice ?: cp
        val pc = closePrice ?: cp
        val ch = change ?: (cp - pc)
        val pct = changePercent ?: if (pc != 0.0) (ch / pc) * 100 else 0.0
        val vol = volume ?: 0L
        val ts = timestamp ?: System.currentTimeMillis()
        return StockQuote(
            symbol = symbol, name = name,
            currentPrice = cp, openPrice = op,
            highPrice = hi, lowPrice = lo,
            closePrice = pc, change = ch,
            changePercent = pct, volume = vol,
            timestamp = ts
        )
    }
}
