package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.data.remote.dto.OpenAIChatMessage
import com.example.funnyexpensetracking.util.Resource

/**
 * 自然语言财务查询Repository接口
 * 支持用户使用自然语言查询个人财务状况
 */
interface FinancialQueryRepository {

    /**
     * 发送自然语言查询并获取AI回复
     * @param userQuestion 用户的自然语言问题
     * @param conversationHistory 对话历史（用于上下文关联，最多保留最近几轮）
     * @return AI的自然语言回复
     */
    suspend fun queryFinancial(
        userQuestion: String,
        conversationHistory: List<OpenAIChatMessage> = emptyList()
    ): Resource<String>
}

