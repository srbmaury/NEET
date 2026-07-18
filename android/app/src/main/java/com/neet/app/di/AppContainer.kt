package com.neet.app.di

import android.content.Context
import androidx.room.Room
import com.neet.app.BuildConfig
import com.neet.app.data.ApiService
import com.neet.app.data.HistoryRepository
import com.neet.app.data.MockTestApiService
import com.neet.app.data.MockTestRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.local.NeetDatabase
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object AppContainer {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // callTimeout alone doesn't override OkHttp's default 10s per-read/write timeouts —
            // a slow-but-still-fast-enough response can hit those before callTimeout ever fires.
            .callTimeout(35, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC),
            )
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }

    val questionRepository: QuestionRepository by lazy { QuestionRepository(apiService) }

    // Mock-test generation is a single long-running call (a few minutes for a full 200-question
    // test) — kept on a separate client/timeout so a genuinely-hung single-question call still
    // fails fast at 35s instead of regressing to a multi-minute hang.
    private val mockTestOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(6, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC),
            )
            .build()
    }

    private val mockTestRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(mockTestOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val mockTestApiService: MockTestApiService by lazy { mockTestRetrofit.create(MockTestApiService::class.java) }

    private val database: NeetDatabase by lazy {
        Room.databaseBuilder(appContext, NeetDatabase::class.java, "neet.db")
            .fallbackToDestructiveMigration(true)
            .build()
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(database.answerDao(), database.mockTestDao())
    }

    val mockTestRepository: MockTestRepository by lazy {
        MockTestRepository(mockTestApiService, database.mockTestDao(), historyRepository)
    }
}
