package com.example.funnyexpensetracking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账户实体
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                      // 账户名称（如：现金、微信、支付宝、银行卡）
    val icon: String = "",                 // 图标名称
    val balance: Double = 0.0,             // 账户余额
    val isDefault: Boolean = false,        // 是否默认账户
    val sortOrder: Int = 0,                // 排序顺序
    val createdAt: Long = System.currentTimeMillis()
)

