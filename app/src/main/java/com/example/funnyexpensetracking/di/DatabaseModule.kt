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

    /**
     * 数据库版本4到5的迁移：添加investments表
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS investments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT NOT NULL,
                    quantity REAL NOT NULL DEFAULT 0.0,
                    investment REAL NOT NULL,
                    currentPrice REAL NOT NULL DEFAULT 0.0,
                    currentValue REAL NOT NULL DEFAULT 0.0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    /**
     * 数据库版本5到6的迁移：为stock_holdings表添加totalCost字段
     * totalCost用于存储购入总价，避免使用单价计算时的误差
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 添加totalCost列，默认值为 shares * purchasePrice
            database.execSQL(
                "ALTER TABLE stock_holdings ADD COLUMN totalCost REAL NOT NULL DEFAULT 0.0"
            )
            // 更新现有数据，计算totalCost = shares * purchasePrice
            database.execSQL(
                "UPDATE stock_holdings SET totalCost = shares * purchasePrice"
            )
        }
    }

    /**
     * 数据库版本6到7的迁移：重构fixed_incomes表
     * - 添加 accumulatedMinutes: 累计生效时间（分钟）
     * - 添加 lastRecordTime: 上次记录时间点
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 添加累计生效时间字段
            database.execSQL(
                "ALTER TABLE fixed_incomes ADD COLUMN accumulatedMinutes INTEGER NOT NULL DEFAULT 0"
            )
            // 添加上次记录时间点字段
            database.execSQL(
                "ALTER TABLE fixed_incomes ADD COLUMN lastRecordTime INTEGER NOT NULL DEFAULT 0"
            )
            // 根据现有的 accumulatedAmount 反推 accumulatedMinutes
            // 这里简单处理：假设 accumulatedAmount 是按月计算的，转换为分钟数
            // 实际上这只是一个近似值，新系统会从此刻开始准确计算
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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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

    @Provides
    @Singleton
    fun provideInvestmentDao(database: AppDatabase): InvestmentDao {
        return database.investmentDao()
    }
}

