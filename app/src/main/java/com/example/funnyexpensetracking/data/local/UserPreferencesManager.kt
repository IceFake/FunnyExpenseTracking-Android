package com.example.funnyexpensetracking.data.local

import android.content.Context
import android.content.SharedPreferences
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

    companion object {
        private const val PREFS_NAME = "funny_expense_tracking_prefs"
        private const val KEY_LAST_SELECTED_ACCOUNT_ID = "last_selected_account_id"
    }
}
