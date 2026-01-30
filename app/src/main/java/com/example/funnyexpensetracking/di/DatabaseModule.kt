package com.example.funnyexpensetracking.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.funnyexpensetracking.data.local.AppDatabase
import com.example.funnyexpensetracking.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库相关的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 数据库版本3到4的迁移：为fixed_incomes表添加accumulatedAmount字段
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE fixed_incomes ADD COLUMN accumulatedAmount REAL NOT NULL DEFAULT 0.0"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "funny_expense_db"
        )
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideFixedIncomeDao(database: AppDatabase): FixedIncomeDao {
        return database.fixedIncomeDao()
    }

    @Provides
    @Singleton
    fun provideStockHoldingDao(database: AppDatabase): StockHoldingDao {
        return database.stockHoldingDao()
    }

    @Provides
    @Singleton
    fun provideAssetSnapshotDao(database: AppDatabase): AssetSnapshotDao {
        return database.assetSnapshotDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }

    @Provides
    @Singleton
    fun provideAssetBaselineDao(database: AppDatabase): AssetBaselineDao {
        return database.assetBaselineDao()
    }
}

