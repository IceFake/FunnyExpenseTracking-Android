package com.example.funnyexpensetracking

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application类，用于初始化Hilt依赖注入
 */
@HiltAndroidApp
class FunnyExpenseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 可以在这里进行其他全局初始化操作
    }
}


