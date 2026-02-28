package com.example.funnyexpensetracking.domain.model

/**
 * 聊天消息模型
 * 用于自然语言财务查询的对话消息
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 聊天角色
 */
enum class ChatRole {
    USER,       // 用户消息
    ASSISTANT,  // AI助手回复
    SYSTEM      // 系统消息（不展示在UI中）
}

