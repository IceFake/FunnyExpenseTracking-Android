package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.StockHoldingEntity
import kotlinx.coroutines.flow.Flow

/**
 * 股票持仓DAO
 */
@Dao
interface StockHoldingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: StockHoldingEntity): Long

    @Update
    suspend fun update(stock: StockHoldingEntity)

    @Delete
    suspend fun delete(stock: StockHoldingEntity)

    @Query("SELECT * FROM stock_holdings WHERE id = :id")
    suspend fun getById(id: Long): StockHoldingEntity?

    @Query("SELECT * FROM stock_holdings WHERE symbol = :symbol")
    suspend fun getBySymbol(symbol: String): StockHoldingEntity?

    @Query("SELECT * FROM stock_holdings ORDER BY createdAt DESC")
    fun getAllStockHoldings(): Flow<List<StockHoldingEntity>>

    @Query("UPDATE stock_holdings SET currentPrice = :price, lastUpdated = :updateTime WHERE symbol = :symbol")
    suspend fun updatePrice(symbol: String, price: Double, updateTime: Long = System.currentTimeMillis())

    @Query("SELECT SUM(shares * currentPrice) FROM stock_holdings")
    suspend fun getTotalStockValue(): Double?

    @Query("SELECT SUM(shares * purchasePrice) FROM stock_holdings")
    suspend fun getTotalStockCost(): Double?

    @Query("DELETE FROM stock_holdings")
    suspend fun deleteAll()
}

