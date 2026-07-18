package com.neet.app.di

import android.content.Context
import androidx.room.Room
import com.neet.app.BuildConfig
import com.neet.app.data.ApiService
import com.neet.app.data.AuthApiService
import com.neet.app.data.AuthRepository
import com.neet.app.data.HealthApiService
import com.neet.app.data.HistoryRepository
import com.neet.app.data.MockTestApiService
import com.neet.app.data.MockTestRepository
import com.neet.app.data.NotesApiService
import com.neet.app.data.NotesRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.RevisionRepository
import com.neet.app.data.SyncApiService
import com.neet.app.data.SyncRepository
import com.neet.app.data.auth.SecureTokenStore
import com.neet.app.data.local.NeetDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
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

    private val secureTokenStore: SecureTokenStore by lazy { SecureTokenStore(appContext) }

    /** Attaches the auth token when one is stored — a no-op for anyone who never logs in, since
     * accounts are strictly additive here (every existing feature already works without one). */
    private val authInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val token = runBlocking { secureTokenStore.tokenFlow().first() }
            val request = if (token != null) {
                chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // callTimeout alone doesn't override OkHttp's default 10s per-read/write timeouts —
            // a slow-but-still-fast-enough response can hit those before callTimeout ever fires.
            .callTimeout(35, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
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

    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }

    val authRepository: AuthRepository by lazy { AuthRepository(authApiService, secureTokenStore) }

    // Notes generation is a single OpenAI call with no verification pass (cheaper/faster than
    // question generation), and results are cached server-side after the first request — the
    // fast client is a fine fit, same as questionApiService.
    val notesApiService: NotesApiService by lazy { retrofit.create(NotesApiService::class.java) }

    val notesRepository: NotesRepository by lazy { NotesRepository(notesApiService) }

    // Mock-test generation is a single long-running call (a few minutes for a full 200-question
    // test) — kept on a separate client/timeout so a genuinely-hung single-question call still
    // fails fast at 35s instead of regressing to a multi-minute hang.
    private val mockTestOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(6, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(authInterceptor)
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

    // Sync payloads grow over time (every practice answer and mock test ever taken) and can
    // exceed what fits comfortably in the fast client's 35s budget — reuses the same
    // long-timeout client built for mock-test generation rather than a third OkHttp instance.
    val syncApiService: SyncApiService by lazy { mockTestRetrofit.create(SyncApiService::class.java) }

    // A free-tier host can take up to ~60s to wake from a cold start — the long-timeout client
    // gives that ping room to actually land instead of erroring out before the wake-up completes.
    val healthApiService: HealthApiService by lazy { mockTestRetrofit.create(HealthApiService::class.java) }

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

    val syncRepository: SyncRepository by lazy { SyncRepository(syncApiService, historyRepository) }

    val revisionRepository: RevisionRepository by lazy { RevisionRepository(database.revisionDao()) }
}
