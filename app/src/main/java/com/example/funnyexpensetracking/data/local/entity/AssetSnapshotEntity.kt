package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 资产快照实体 - 用于记录资产历史变化
 */
@Entity(tableName = "asset_snapshots")
data class AssetSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val totalAsset: Double,                // 总资产
    val cashAsset: Double,                 // 现金资产（不含股票）
    val stockAsset: Double,                // 股票资产
    val timestamp: Long = System.currentTimeMillis() // 快照时间
)

