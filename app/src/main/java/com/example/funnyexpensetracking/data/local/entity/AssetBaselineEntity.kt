package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 资产基准值实体
 * 用于存储某个时间点的资产基准值，配合固定收支计算实时资产
 */
@Entity(tableName = "asset_baseline")
data class AssetBaselineEntity(
    @PrimaryKey
    val id: Int = 1,  // 只存储一条记录
    val baselineTimestamp: Long,  // 基准时间戳（精确到分钟）
    val baselineAmount: Double,   // 基准资产值
    val updatedAt: Long = System.currentTimeMillis()
)

