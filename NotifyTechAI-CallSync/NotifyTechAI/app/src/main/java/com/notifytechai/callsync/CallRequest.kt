package com.notifytechai.callsync

import com.google.gson.annotations.SerializedName

/**
 * CallRequest — maps exactly to what the Apps Script webhook expects.
 *
 * Apps Script reads:
 *   data.agent, data.number, data.duration, data.type
 *
 * @param agent    Agent name or ID (future: from login session)
 * @param number   Phone number that was called / called from
 * @param duration Call duration in seconds (as String from CallLog API)
 * @param type     "OUTGOING", "INCOMING", or "MISSED"
 * @param timestamp ISO timestamp of when the call occurred
 */
data class CallRequest(

    @SerializedName("agent")
    val agent: String,

    @SerializedName("number")
    val number: String,

    @SerializedName("duration")
    val duration: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("timestamp")
    val timestamp: String = java.time.Instant.now().toString()
)
