package com.example.funnyexpensetracking.data.local.dao
}
    suspend fun deleteAll()
    @Query("DELETE FROM transactions")

    suspend fun getCategoriesByType(type: TransactionType): List<String>
    @Query("SELECT DISTINCT category FROM transactions WHERE type = :type")

    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Double?
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")

    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")

    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")

    fun getAllTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY date DESC")

    suspend fun getById(id: Long): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE id = :id")

    suspend fun delete(transaction: TransactionEntity)
    @Delete

    suspend fun update(transaction: TransactionEntity)
    @Update

    suspend fun insert(transaction: TransactionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)

interface TransactionDao {
@Dao
 */
 * 交易记录DAO
/**

import kotlinx.coroutines.flow.Flow
import com.example.funnyexpensetracking.data.local.entity.TransactionType
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import androidx.room.*


