package com.example.funnyexpensetracking.data.local

import androidx.room.TypeConverter
import com.example.funnyexpensetracking.data.local.entity.*

/**
 * Room数据库类型转换器
 */
class Converters {

    // ========== TransactionType 转换 ==========
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    // ========== SyncStatus 转换 ==========
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    // ========== FixedIncomeType 转换 ==========
    @TypeConverter
    fun fromFixedIncomeType(type: FixedIncomeType): String = type.name

    @TypeConverter
    fun toFixedIncomeType(value: String): FixedIncomeType = FixedIncomeType.valueOf(value)

    // ========== FixedIncomeFrequency 转换 ==========
    @TypeConverter
    fun fromFixedIncomeFrequency(frequency: FixedIncomeFrequency): String = frequency.name

    @TypeConverter
    fun toFixedIncomeFrequency(value: String): FixedIncomeFrequency = FixedIncomeFrequency.valueOf(value)
}

