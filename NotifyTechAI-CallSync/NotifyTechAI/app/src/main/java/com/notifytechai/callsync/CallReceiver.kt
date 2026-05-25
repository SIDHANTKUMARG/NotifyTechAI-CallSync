package com.notifytechai.callsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * CallReceiver — listens for phone state broadcasts.
 *
 * ┌─────────────┐   ┌──────────────┐   ┌───────────────┐
 * │  RINGING    │ → │  OFF_HOOK    │ → │     IDLE      │
 * │ (incoming)  │   │ (in call)    │   │ (call ended)  │
 * └─────────────┘   └──────────────┘   └───────────────┘
 *
 * We act on IDLE — that is when the call log is finalized
 * and the duration is written by the system.
 *
 * State machine:
 *   - Track previous state to distinguish:
 *       IDLE after OFF_HOOK → real call (read log)
 *       IDLE after RINGING  → missed call (read log)
 *       IDLE cold            → app boot, ignore
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"

        // Shared prefs key to persist state across system kills
        private const val PREFS = "call_prefs"
        private const val KEY_PREV_STATE = "prev_state"
        private const val KEY_OUTGOING = "outgoing_number"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // ── Capture outgoing number before the call starts ────────────────
        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: ""
            prefs.edit().putString(KEY_OUTGOING, number).apply()
            Log.d(TAG, "Outgoing call to: $number")
            return
        }

        // ── Phone state change ────────────────────────────────────────────
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {

            val currentState = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val previousState = prefs.getString(KEY_PREV_STATE, TelephonyManager.EXTRA_STATE_IDLE)

            Log.d(TAG, "State: $previousState → $currentState")

            when (currentState) {

                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Phone is ringing — incoming call
                    prefs.edit().putString(KEY_PREV_STATE, currentState).apply()
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call was answered / outgoing call started
                    prefs.edit().putString(KEY_PREV_STATE, currentState).apply()
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended — NOW the call log has the final duration
                    val wasOffHook = previousState == TelephonyManager.EXTRA_STATE_OFFHOOK
                    val wasRinging = previousState == TelephonyManager.EXTRA_STATE_RINGING

                    if (wasOffHook || wasRinging) {
                        // Small delay — system takes ~500ms to finalize the log
                        CoroutineScope(Dispatchers.IO).launch {
                            kotlinx.coroutines.delay(1000)
                            readLatestCallLog(context)
                        }
                    }

                    // Reset state
                    prefs.edit()
                        .putString(KEY_PREV_STATE, currentState)
                        .remove(KEY_OUTGOING)
                        .apply()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Read the most recent call log entry
    // ─────────────────────────────────────────────────────────────────────
    private fun readLatestCallLog(context: Context) {

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"  // Most recent first
        )

        cursor?.use {
            if (it.moveToFirst()) {

                val number   = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val duration = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                val typeInt  = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))

                val callType = when (typeInt) {
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.MISSED_TYPE   -> "MISSED"
                    else                         -> "UNKNOWN"
                }

                Log.d(TAG, "Call log → $callType | $number | ${duration}s")

                // Fire and forget — sync to server
                CoroutineScope(Dispatchers.IO).launch {
                    SyncWorker.enqueue(
                        context  = context,
                        number   = number,
                        duration = duration,
                        type     = callType
                    )
                }
            }
        }
    }
}
