package com.example.funnyexpensetracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.funnyexpensetracking.data.local.dao.*
import com.example.funnyexpensetracking.data.local.entity.*

/**
 * Room数据库定义
 */
@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        FixedIncomeEntity::class,
        StockHoldingEntity::class,
        AssetSnapshotEntity::class,
        SyncMetadataEntity::class,
        AssetBaselineEntity::class,
        InvestmentEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun fixedIncomeDao(): FixedIncomeDao
    abstract fun stockHoldingDao(): StockHoldingDao
    abstract fun assetSnapshotDao(): AssetSnapshotDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun assetBaselineDao(): AssetBaselineDao
    abstract fun investmentDao(): InvestmentDao
}

