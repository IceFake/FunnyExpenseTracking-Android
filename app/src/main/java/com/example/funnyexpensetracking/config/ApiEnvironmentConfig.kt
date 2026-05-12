package com.example.funnyexpensetracking.config

/**
 * 应用级 API 环境配置
 *
 * 说明：
 * - 将是否使用测试（本地）环境的开关放在此处统一管理。
 * - 设为 true 表示使用测试（local）环境，false 表示使用线上（production）环境。
 * - 不在 UI 提供切换开关，避免用户在界面误操作。若需要切换环境，修改此处变量并重新安装/重启应用。
 */
object ApiEnvironmentConfig {
    /**
     * 是否使用测试（本地）API环境。
     * 修改为 true 表示使用本地/测试环境；false 表示使用线上环境。
     */
    var IS_TEST_ENV: Boolean = true
}

