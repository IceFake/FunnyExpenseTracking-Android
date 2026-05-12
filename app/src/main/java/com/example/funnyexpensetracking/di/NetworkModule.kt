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
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 网络相关的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/"
    private const val SINA_FINANCE_BASE_URL = "https://hq.sinajs.cn/"
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

    /**
     * Yahoo Finance API 专用 OkHttpClient
     * 添加必要的请求头以避免被拒绝
     */
    @Provides
    @Singleton
    @Named("yahooFinanceClient")
    fun provideYahooFinanceOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 添加 Yahoo Finance 需要的请求头
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
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

    @Provides
    @Singleton
    @Named("yahooFinance")
    fun provideYahooFinanceRetrofit(
        @Named("yahooFinanceClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(YAHOO_FINANCE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 新浪财经 API 专用 OkHttpClient
     */
    @Provides
    @Singleton
    @Named("sinaFinanceClient")
    fun provideSinaFinanceOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 添加新浪财经需要的请求头
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Referer", "https://finance.sina.com.cn/")
                .addHeader("Accept", "*/*")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("sinaFinance")
    fun provideSinaFinanceRetrofit(@Named("sinaFinanceClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SINA_FINANCE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    /**
     * DeepSeek API 专用 OkHttpClient
     * 不添加日志拦截器以避免泄露API密钥
     * 使用更长的超时时间（AI生成可能较慢）
     * 添加重试拦截器处理瞬时错误（429/500/503）
     */
    @Provides
    @Singleton
    @Named("deepSeekClient")
    fun provideDeepSeekOkHttpClient(): OkHttpClient {
        // 重试拦截器：处理限流和服务端瞬时错误
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
            .readTimeout(120, TimeUnit.SECONDS)  // AI生成可能需要较长时间
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
    fun provideYahooFinanceApiService(@Named("yahooFinance") retrofit: Retrofit): YahooFinanceApiService {
        return retrofit.create(YahooFinanceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSinaFinanceApiService(@Named("sinaFinance") retrofit: Retrofit): SinaFinanceApiService {
        return retrofit.create(SinaFinanceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAccountApiService(@Named("default") retrofit: Retrofit): AccountApiService {
        return retrofit.create(AccountApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDeepSeekApiService(@Named("deepSeek") retrofit: Retrofit): DeepSeekApiService {
        return retrofit.create(DeepSeekApiService::class.java)
    }

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

