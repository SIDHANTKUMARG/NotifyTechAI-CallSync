package com.notifytechai.callsync

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * RetrofitClient — singleton that builds the Retrofit instance once.
 *
 * HOW TO USE:
 *   RetrofitClient.api.sendCallData(body)
 *
 * WEBHOOK URL:
 *   Replace WEBHOOK_BASE_URL with your Apps Script deployment URL.
 *   Format: https://script.google.com/macros/s/<DEPLOYMENT_ID>/
 *   (must end with a trailing slash)
 */
object RetrofitClient {

    // ── ⚠️  Replace this with your real Apps Script URL ──────────────────
    private const val WEBHOOK_BASE_URL =
        "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID_HERE/"
    // ─────────────────────────────────────────────────────────────────────

    /**
     * OkHttp client with:
     *  - Logging (debug builds only — remove in production)
     *  - 30s connect / read / write timeouts
     */
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit instance — built once, reused everywhere.
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WEBHOOK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * The actual API interface — use this to make calls.
     */
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
