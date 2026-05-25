package com.notifytechai.callsync

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * ApiService — Retrofit interface.
 *
 * The Apps Script URL looks like:
 *   https://script.google.com/macros/s/AKfycbxxxx/exec
 *
 * Retrofit baseUrl is set to:
 *   https://script.google.com/macros/s/AKfycbxxxx/
 *
 * So @POST("exec") resolves to the full URL.
 *
 * Note: suspend → called from a coroutine (no blocking the UI thread).
 */
interface ApiService {

    @POST("exec")
    suspend fun sendCallData(
        @Body body: CallRequest
    ): retrofit2.Response<Map<String, Any>>
}
