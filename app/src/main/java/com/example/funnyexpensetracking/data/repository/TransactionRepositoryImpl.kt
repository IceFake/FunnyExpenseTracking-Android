package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.TransactionDao
import com.example.funnyexpensetracking.data.local.entity.TransactionEntity
import com.example.funnyexpensetracking.data.local.entity.TransactionType as EntityTransactionType
import com.example.funnyexpensetracking.data.remote.api.ExpenseApiService
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易记录Repository实现类
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val expenseApiService: ExpenseApiService
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type.toEntityType()).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getById(id)?.toDomainModel()
    }

    override suspend fun addTransaction(transaction: Transaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }

    override suspend fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double {
        return transactionDao.getTotalByTypeAndDateRange(type.toEntityType(), startDate, endDate) ?: 0.0
    }

    override suspend fun getCategoriesByType(type: TransactionType): List<String> {
        return transactionDao.getCategoriesByType(type.toEntityType())
    }

    override suspend fun syncWithServer() {
        // TODO: 实现与服务器的数据同步
    }

    private fun TransactionEntity.toDomainModel(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = type.toDomainType(),
            category = category,
            accountId = accountId,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type.toEntityType(),
            category = category,
            accountId = accountId,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    private fun TransactionType.toEntityType(): EntityTransactionType {
        return when (this) {
            TransactionType.INCOME -> EntityTransactionType.INCOME
            TransactionType.EXPENSE -> EntityTransactionType.EXPENSE
        }
    }

    private fun EntityTransactionType.toDomainType(): TransactionType {
        return when (this) {
            EntityTransactionType.INCOME -> TransactionType.INCOME
            EntityTransactionType.EXPENSE -> TransactionType.EXPENSE
        }
    }
}

