package com.example.funnyexpensetracking.data.repository

import com.example.funnyexpensetracking.data.local.dao.AccountDao
import com.example.funnyexpensetracking.data.local.entity.AccountEntity
import com.example.funnyexpensetracking.data.local.entity.SyncStatus
import com.example.funnyexpensetracking.data.sync.SyncManager
import com.example.funnyexpensetracking.domain.model.Account
import com.example.funnyexpensetracking.domain.repository.AccountRepository
import com.example.funnyexpensetracking.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 账户Repository实现类
 * 采用离线优先策略
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccountsExcludeStatus(SyncStatus.PENDING_DELETE)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getById(id)?.toDomainModel()
    }

    override suspend fun getDefaultAccount(): Account? {
        return accountDao.getDefaultAccount()?.toDomainModel()
    }

    override suspend fun addAccount(account: Account): Long {
        val entity = account.toEntity().copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        val localId = accountDao.insert(entity)
        trySync()
        return localId
    }

    override suspend fun updateAccount(account: Account) {
        val existingEntity = accountDao.getById(account.id)
        val entity = account.toEntity().copy(
            serverId = existingEntity?.serverId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        accountDao.update(entity)
        trySync()
    }

    override suspend fun deleteAccount(account: Account) {
        val entity = accountDao.getById(account.id)
        if (entity != null) {
            if (entity.serverId != null) {
                accountDao.markAsDeleted(account.id)
                trySync()
            } else {
                accountDao.delete(entity)
            }
        }
    }

    override suspend fun updateBalance(accountId: Long, amount: Double) {
        accountDao.updateBalance(accountId, amount)
        trySync()
    }

    override suspend fun setDefaultAccount(accountId: Long) {
        accountDao.clearDefaultAccount()
        accountDao.setDefaultAccount(accountId)
        trySync()
    }

    override suspend fun getTotalBalance(): Double {
        return accountDao.getTotalBalance() ?: 0.0
    }

    override suspend fun syncWithServer() {
        syncManager.syncAccounts()
    }

    private suspend fun trySync() {
        if (networkMonitor.isNetworkAvailable()) {
            syncManager.syncAccounts()
        }
    }

    // ========== 转换函数 ==========

    private fun AccountEntity.toDomainModel(): Account {
        return Account(
            id = id,
            name = name,
            icon = icon,
            balance = balance,
            isDefault = isDefault,
            sortOrder = sortOrder
        )
    }

    private fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            id = id,
            name = name,
            icon = icon,
            balance = balance,
            isDefault = isDefault,
            sortOrder = sortOrder,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
    }
}

