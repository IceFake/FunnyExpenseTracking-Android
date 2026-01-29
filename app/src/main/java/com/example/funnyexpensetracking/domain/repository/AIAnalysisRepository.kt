package com.example.funnyexpensetracking.domain.repository

import com.example.funnyexpensetracking.domain.model.AIAnalysisResult
import com.example.funnyexpensetracking.domain.model.Suggestion
import com.example.funnyexpensetracking.util.Resource

/**
 * AI分析Repository接口
 */
interface AIAnalysisRepository {

    /**
     * 请求AI分析用户习惯
     */
    suspend fun analyzeHabits(): Resource<AIAnalysisResult>

    /**
     * 获取AI建议
     */
    suspend fun getSuggestions(): Resource<List<Suggestion>>

    /**
     * 获取历史分析结果
     */
    suspend fun getAnalysisHistory(limit: Int = 10): Resource<List<AIAnalysisResult>>

    /**
     * 根据ID获取分析结果
     */
    suspend fun getAnalysisById(analysisId: String): Resource<AIAnalysisResult>
}

