package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.StockHoldingDao
import com.example.funnyexpensetracking.data.local.entity.StockHoldingEntity
import com.example.funnyexpensetracking.data.remote.api.StockApiService
import com.example.funnyexpensetracking.data.remote.dto.BatchQuoteRequest
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

    override suspend fun getStockQuote(symbol: String): Resource<StockQuote> {
        return try {
            val response = stockApiService.getQuote(symbol)
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                stockHoldingDao.updatePrice(symbol, dto.currentPrice)
                Resource.Success(dto.toDomainModel())
            } else {
                Resource.Error(response.message() ?: "获取行情失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
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

    override suspend fun searchStock(keyword: String): Resource<List<StockQuote>> {
        return try {
            val response = stockApiService.searchStock(keyword)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(
                    response.body()!!.data!!.map { searchResult ->
                        StockQuote(
                            symbol = searchResult.symbol,
                            name = searchResult.name,
                            currentPrice = 0.0,
                            openPrice = 0.0,
                            highPrice = 0.0,
                            lowPrice = 0.0,
                            closePrice = 0.0,
                            change = 0.0,
                            changePercent = 0.0,
                            volume = 0,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                )
            } else {
                Resource.Error(response.message() ?: "搜索股票失败")
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

    override suspend fun getRealtimeQuote(symbol: String): Resource<StockQuote> {
        return try {
            val response = stockApiService.getRealtimeQuote(symbol)
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                stockHoldingDao.updatePrice(symbol, dto.currentPrice)
                Resource.Success(dto.toDomainModel())
            } else {
                Resource.Error(response.message() ?: "获取实时行情失败")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "网络错误")
        }
    }

    private fun StockHoldingEntity.toDomainModel(): StockHolding {
        return StockHolding(id = id, symbol = symbol, name = name, shares = shares, purchasePrice = purchasePrice, purchaseDate = purchaseDate, currentPrice = currentPrice, lastUpdated = lastUpdated)
    }

    private fun StockHolding.toEntity(): StockHoldingEntity {
        return StockHoldingEntity(id = id, symbol = symbol, name = name, shares = shares, purchasePrice = purchasePrice, purchaseDate = purchaseDate, currentPrice = currentPrice, lastUpdated = lastUpdated, createdAt = System.currentTimeMillis())
    }

    private fun com.example.funnyexpensetracking.data.remote.dto.StockQuoteDto.toDomainModel(): StockQuote {
        return StockQuote(symbol = symbol, name = name, currentPrice = currentPrice, openPrice = openPrice, highPrice = highPrice, lowPrice = lowPrice, closePrice = closePrice, change = change, changePercent = changePercent, volume = volume, timestamp = timestamp)
    }
}

