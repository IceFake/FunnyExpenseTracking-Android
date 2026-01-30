package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeEntity
import com.example.funnyexpensetracking.data.local.entity.FixedIncomeType
import kotlinx.coroutines.flow.Flow

/**
 * 固定收支DAO
 */
@Dao
interface FixedIncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fixedIncome: FixedIncomeEntity): Long

    @Update
    suspend fun update(fixedIncome: FixedIncomeEntity)

    @Delete
    suspend fun delete(fixedIncome: FixedIncomeEntity)

    @Query("SELECT * FROM fixed_incomes WHERE id = :id")
    suspend fun getById(id: Long): FixedIncomeEntity?

    @Query("SELECT * FROM fixed_incomes WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveFixedIncomes(): Flow<List<FixedIncomeEntity>>

    @Query("SELECT * FROM fixed_incomes ORDER BY createdAt DESC")
    fun getAllFixedIncomes(): Flow<List<FixedIncomeEntity>>

    @Query("SELECT * FROM fixed_incomes WHERE type = :type AND isActive = 1")
    fun getActiveByType(type: FixedIncomeType): Flow<List<FixedIncomeEntity>>

    @Query("UPDATE fixed_incomes SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)

    @Query("UPDATE fixed_incomes SET accumulatedAmount = accumulatedAmount + :amount WHERE id = :id")
    suspend fun addToAccumulatedAmount(id: Long, amount: Double)

    @Query("UPDATE fixed_incomes SET accumulatedAmount = :amount WHERE id = :id")
    suspend fun setAccumulatedAmount(id: Long, amount: Double)

    /**
     * 获取所有固定收入的累计总额
     */
    @Query("SELECT COALESCE(SUM(accumulatedAmount), 0.0) FROM fixed_incomes WHERE type = 'INCOME'")
    suspend fun getTotalAccumulatedIncome(): Double

    /**
     * 获取所有固定支出的累计总额
     */
    @Query("SELECT COALESCE(SUM(accumulatedAmount), 0.0) FROM fixed_incomes WHERE type = 'EXPENSE'")
    suspend fun getTotalAccumulatedExpense(): Double

    @Query("DELETE FROM fixed_incomes")
    suspend fun deleteAll()
}

