package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.InvestmentCategory
import com.example.funnyexpensetracking.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * 投资DAO
 */
@Dao
interface InvestmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(investment: InvestmentEntity): Long

    @Update
    suspend fun update(investment: InvestmentEntity)

    @Delete
    suspend fun delete(investment: InvestmentEntity)

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getById(id: Long): InvestmentEntity?

    @Query("SELECT * FROM investments ORDER BY createdAt DESC")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: InvestmentCategory): Flow<List<InvestmentEntity>>

    /**
     * 根据描述查找投资（用于合并相同条目）
     */
    @Query("SELECT * FROM investments WHERE description = :description AND category = :category LIMIT 1")
    suspend fun findByDescriptionAndCategory(description: String, category: InvestmentCategory): InvestmentEntity?

    /**
     * 更新股票当前价格
     */
    @Query("UPDATE investments SET currentPrice = :price, updatedAt = :updatedAt WHERE description = :stockCode AND category = 'STOCK'")
    suspend fun updateStockPrice(stockCode: String, price: Double, updatedAt: Long = System.currentTimeMillis())

    /**
     * 获取所有股票代码
     */
    @Query("SELECT DISTINCT description FROM investments WHERE category = 'STOCK'")
    suspend fun getAllStockCodes(): List<String>

    @Query("DELETE FROM investments")
    suspend fun deleteAll()

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

