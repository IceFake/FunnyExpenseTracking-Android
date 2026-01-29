package com.example.funnyexpensetracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.funnyexpensetracking.data.local.dao.*
import com.example.funnyexpensetracking.data.local.entity.*

/**
 * Room数据库定义
 */
@Database(
    entities = [
        TransactionEntity::class,
        FixedIncomeEntity::class,
        StockHoldingEntity::class,
        AssetSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun fixedIncomeDao(): FixedIncomeDao
    abstract fun stockHoldingDao(): StockHoldingDao
    abstract fun assetSnapshotDao(): AssetSnapshotDao
}

