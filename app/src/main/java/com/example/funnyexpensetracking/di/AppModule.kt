package com.example.funnyexpensetracking.di

import android.content.Context
import com.example.funnyexpensetracking.FunnyExpenseApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用级别的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplication(@ApplicationContext context: Context): FunnyExpenseApp {
        return context as FunnyExpenseApp
    }
}

