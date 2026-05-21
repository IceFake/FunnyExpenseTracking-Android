package com.example.funnyexpensetracking.di

import com.example.funnyexpensetracking.BuildConfig
import com.example.funnyexpensetracking.config.ApiEnvironmentConfig
import com.example.funnyexpensetracking.data.remote.AuthInterceptor
import com.example.funnyexpensetracking.data.remote.api.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.funnyexpensetracking.data.remote.TokenAuthenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 网络相关的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/"

    /**
     * 仅用于 auth/refresh：无 Bearer 拦截、无 [TokenAuthenticator]，避免 401 死循环。
     */
    @Provides
    @Singleton
    @Named("authBareClient")
    fun provideAuthBareOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("backendBaseUrl")
    fun provideBackendBaseUrl(): String {
        return if (ApiEnvironmentConfig.IS_TEST_ENV) {
            BuildConfig.LOCAL_API_BASE_URL
        } else {
            BuildConfig.API_BASE_URL
        }
    }

    @Provides
    @Singleton
    @Named("authBare")
    fun provideAuthBareRetrofit(
        @Named("authBareClient") okHttpClient: OkHttpClient,
        @Named("lenientGson") gson: Gson,
        @Named("backendBaseUrl") baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthTokenRefreshApiService(@Named("authBare") retrofit: Retrofit): AuthTokenRefreshApiService {
        return retrofit.create(AuthTokenRefreshApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .authenticator(tokenAuthenticator)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("default")
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @Named("lenientGson") gson: Gson,
        @Named("backendBaseUrl") baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // ==================== DeepSeek AI ====================

    /**
     * DeepSeek API 专用 OkHttpClient
     */
    @Provides
    @Singleton
    @Named("deepSeekClient")
    fun provideDeepSeekOkHttpClient(): OkHttpClient {
        val retryInterceptor = Interceptor { chain ->
            var response = chain.proceed(chain.request())
            var tryCount = 0
            val maxRetries = 2

            while (!response.isSuccessful && tryCount < maxRetries
                && response.code in listOf(429, 500, 503)
            ) {
                tryCount++
                val waitTime = (Math.pow(2.0, tryCount.toDouble()) * 1000).toLong()
                try {
                    Thread.sleep(waitTime)
                } catch (_: InterruptedException) {
                    break
                }
                response.close()
                response = chain.proceed(chain.request())
            }
            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("deepSeek")
    fun provideDeepSeekRetrofit(
        @Named("deepSeekClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DEEPSEEK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideDeepSeekApiService(@Named("deepSeek") retrofit: Retrofit): DeepSeekApiService {
        return retrofit.create(DeepSeekApiService::class.java)
    }

    // ==================== 后端 API Services ====================

    @Provides
    @Singleton
    fun provideAuthApiService(@Named("default") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideExpenseApiService(@Named("default") retrofit: Retrofit): ExpenseApiService {
        return retrofit.create(ExpenseApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStatisticsApiService(@Named("default") retrofit: Retrofit): StatisticsApiService {
        return retrofit.create(StatisticsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAIAnalysisApiService(@Named("default") retrofit: Retrofit): AIAnalysisApiService {
        return retrofit.create(AIAnalysisApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStockApiService(@Named("default") retrofit: Retrofit): StockApiService {
        return retrofit.create(StockApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAccountApiService(@Named("default") retrofit: Retrofit): AccountApiService {
        return retrofit.create(AccountApiService::class.java)
    }

    // ==================== Gson ====================

    @Provides
    @Singleton
    @Named("lenientGson")
    fun provideLenientGson(): Gson {
        return GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .create()
    }
}
