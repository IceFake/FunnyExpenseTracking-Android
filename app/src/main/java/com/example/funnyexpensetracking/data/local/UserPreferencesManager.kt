package com.example.funnyexpensetracking.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.funnyexpensetracking.domain.model.AIAnalysisResult
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好设置管理器
 *
 * 安全策略：
 * - 敏感数据（API Key）使用 [EncryptedSharedPreferences] 加密存储，
 *   基于 Android Keystore 的 AES256-GCM 加密，密钥由系统管理，
 *   即使设备被 root 也无法直接读取明文。
 * - 非敏感数据（UI 偏好、分析缓存）使用普通 SharedPreferences 存储，
 *   避免不必要的加解密开销。
 * - 如果 EncryptedSharedPreferences 初始化失败（低版本设备等极端情况），
 *   自动降级到普通 SharedPreferences 并输出日志警告。
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    /** 普通 SharedPreferences — 存储非敏感偏好 */
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /** 加密 SharedPreferences — 存储 API Key 等敏感信息 */
    private val securePrefs: SharedPreferences = createSecurePrefs(context)

    // ========================= 账户偏好（非敏感） =========================

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

    // ========================= API Key（敏感，加密存储） =========================

    /**
     * 保存DeepSeek API Key（加密存储）
     */
    fun saveDeepSeekApiKey(apiKey: String) {
        securePrefs.edit().putString(KEY_DEEPSEEK_API_KEY, apiKey).apply()
    }

    /**
     * 获取DeepSeek API Key（加密读取）
     * @return API Key，如果未设置则返回空字符串
     */
    fun getDeepSeekApiKey(): String {
        // 优先从加密存储读取
        val secureKey = securePrefs.getString(KEY_DEEPSEEK_API_KEY, null)
        if (!secureKey.isNullOrBlank()) {
            return secureKey
        }

        // 兼容旧版：检查普通 SharedPreferences 中是否有历史数据，如有则迁移
        val legacyKey = prefs.getString(KEY_DEEPSEEK_API_KEY, null)
        if (!legacyKey.isNullOrBlank()) {
            Log.d(TAG, "检测到旧版 API Key，正在迁移到加密存储...")
            securePrefs.edit().putString(KEY_DEEPSEEK_API_KEY, legacyKey).apply()
            prefs.edit().remove(KEY_DEEPSEEK_API_KEY).apply()
            return legacyKey
        }

        return ""
    }

    /**
     * 检查是否已配置DeepSeek API Key
     */
    fun hasDeepSeekApiKey(): Boolean {
        return getDeepSeekApiKey().isNotBlank()
    }

    // ========================= AI 分析缓存（非敏感） =========================

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

    // ========================= 内部方法 =========================

    /**
     * 创建 EncryptedSharedPreferences，失败时降级到普通 SharedPreferences
     */
    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences 初始化失败，降级到普通存储", e)
            context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val TAG = "UserPreferencesManager"
        private const val PREFS_NAME = "funny_expense_tracking_prefs"
        private const val SECURE_PREFS_NAME = "funny_expense_tracking_secure_prefs"
        private const val KEY_LAST_SELECTED_ACCOUNT_ID = "last_selected_account_id"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_LAST_AI_ANALYSIS_RESULT = "last_ai_analysis_result"
        private val gson = Gson()
    }
}
