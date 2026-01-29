package com.example.funnyexpensetracking.data.local.dao

import androidx.room.*
import com.example.funnyexpensetracking.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * 账户DAO
 */
@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): AccountEntity?

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amount: Double)

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultAccount()

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :accountId")
    suspend fun setDefaultAccount(accountId: Long)

    @Query("SELECT SUM(balance) FROM accounts")
    suspend fun getTotalBalance(): Double?

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}




