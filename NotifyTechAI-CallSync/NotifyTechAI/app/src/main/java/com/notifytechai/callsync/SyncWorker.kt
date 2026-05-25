package com.notifytechai.callsync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * SyncWorker — uses WorkManager to sync call data to the webhook.
 *
 * WHY WorkManager instead of a direct coroutine?
 * ───────────────────────────────────────────────
 * • Survives process death (Chinese OEM phone killers)
 * • Automatic retry with exponential back-off if network fails
 * • Respects network constraints (won't waste data on offline)
 * • Guaranteed execution — even across reboots
 *
 * RETRY POLICY:
 *   If the webhook call fails, WorkManager retries up to 3 times
 *   with exponential back-off (30s → 60s → 120s).
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"

        // Input data keys
        const val KEY_AGENT    = "agent"
        const val KEY_NUMBER   = "number"
        const val KEY_DURATION = "duration"
        const val KEY_TYPE     = "type"

        // Max retry attempts before giving up
        private const val MAX_RETRIES = 3

        /**
         * Enqueue a one-time sync job.
         * Call this from CallReceiver after reading the call log.
         *
         * @param context  Application context
         * @param number   Phone number
         * @param duration Duration in seconds
         * @param type     OUTGOING / INCOMING / MISSED
         * @param agent    Agent identifier (default: device name for MVP)
         */
        fun enqueue(
            context: Context,
            number: String,
            duration: String,
            type: String,
            agent: String = android.os.Build.MODEL   // Replace with real login in Phase 2
        ) {
            val inputData = workDataOf(
                KEY_AGENT    to agent,
                KEY_NUMBER   to number,
                KEY_DURATION to duration,
                KEY_TYPE     to type
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // Only sync with internet
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS           // First retry after 30s
                )
                .build()

            WorkManager.getInstance(context)
                .enqueue(request)

            Log.d(TAG, "SyncWorker enqueued for $type call to $number")
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Worker execution — runs on a background thread automatically
    // ─────────────────────────────────────────────────────────────────────
    override suspend fun doWork(): Result {

        val agent    = inputData.getString(KEY_AGENT)    ?: return Result.failure()
        val number   = inputData.getString(KEY_NUMBER)   ?: return Result.failure()
        val duration = inputData.getString(KEY_DURATION) ?: return Result.failure()
        val type     = inputData.getString(KEY_TYPE)     ?: return Result.failure()

        Log.d(TAG, "Syncing: $type | $number | ${duration}s")

        return try {
            val body = CallRequest(
                agent    = agent,
                number   = number,
                duration = duration,
                type     = type
            )

            val response = RetrofitClient.api.sendCallData(body)

            if (response.isSuccessful) {
                Log.d(TAG, "✅ Sync successful")
                Result.success()
            } else {
                Log.w(TAG, "⚠️ Server error ${response.code()} — retrying")
                retryOrFail()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error: ${e.message}")
            retryOrFail()
        }
    }

    private fun retryOrFail(): Result {
        return if (runAttemptCount < MAX_RETRIES) {
            Result.retry()
        } else {
            Log.e(TAG, "Max retries reached — giving up")
            Result.failure()
        }
    }
}
