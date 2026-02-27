package com.example.funnyexpensetracking.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.funnyexpensetracking.domain.model.AIAnalysisResult
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好设置管理器
 * 使用 SharedPreferences 持久化保存用户的偏好设置
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 保存用户上次选择的账户ID
     */
    fun saveLastSelectedAccountId(accountId: Long) {
        prefs.edit().putLong(KEY_LAST_SELECTED_ACCOUNT_ID, accountId).apply()
    }

    /**
     * 获取用户上次选择的账户ID
     * @return 上次选择的账户ID，如果没有则返回 0
     */
    fun getLastSelectedAccountId(): Long {
        return prefs.getLong(KEY_LAST_SELECTED_ACCOUNT_ID, 0L)
    }

    /**
     * 保存DeepSeek API Key
     */
    fun saveDeepSeekApiKey(apiKey: String) {
        prefs.edit().putString(KEY_DEEPSEEK_API_KEY, apiKey).apply()
    }

    /**
     * 获取DeepSeek API Key
     * @return API Key，如果未设置则返回空字符串
     */
    fun getDeepSeekApiKey(): String {
        return prefs.getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
    }

    /**
     * 检查是否已配置DeepSeek API Key
     */
    fun hasDeepSeekApiKey(): Boolean {
        return getDeepSeekApiKey().isNotBlank()
    }

    /**
     * 保存上次AI分析结果（JSON序列化）
     */
    fun saveLastAIAnalysisResult(result: AIAnalysisResult) {
        val json = gson.toJson(result)
        prefs.edit().putString(KEY_LAST_AI_ANALYSIS_RESULT, json).apply()
    }

    /**
     * 获取上次AI分析结果
     * @return 上次分析结果，如果没有则返回null
     */
    fun getLastAIAnalysisResult(): AIAnalysisResult? {
        val json = prefs.getString(KEY_LAST_AI_ANALYSIS_RESULT, null) ?: return null
        return try {
            gson.fromJson(json, AIAnalysisResult::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否有缓存的AI分析结果
     */
    fun hasLastAIAnalysisResult(): Boolean {
        return prefs.contains(KEY_LAST_AI_ANALYSIS_RESULT)
    }

    companion object {
        private const val PREFS_NAME = "funny_expense_tracking_prefs"
        private const val KEY_LAST_SELECTED_ACCOUNT_ID = "last_selected_account_id"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_LAST_AI_ANALYSIS_RESULT = "last_ai_analysis_result"
        private val gson = Gson()
    }
}
