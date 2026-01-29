package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 同步元数据实体 - 记录各表的同步状态
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val tableName: String,                 // 表名
    val lastSyncTimestamp: Long = 0,       // 上次成功同步时间
    val lastSyncAttempt: Long = 0,         // 上次同步尝试时间
    val pendingCount: Int = 0,             // 待同步记录数
    val lastError: String? = null          // 上次同步错误信息
)

