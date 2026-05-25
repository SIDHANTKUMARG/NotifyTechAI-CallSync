package com.notifytechai.callsync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * CallSyncService — foreground service that keeps the app alive.
 *
 * WHY A FOREGROUND SERVICE?
 * ─────────────────────────
 * Android 8.0+ aggressively kills background apps to save battery.
 * On Xiaomi, Oppo, Vivo, and Realme phones this is even more aggressive.
 * A foreground service shows a persistent notification and prevents the
 * OS from killing the process — meaning CallReceiver keeps firing.
 *
 * The notification is minimal and non-intrusive.
 * Users can collapse it — it stays in the status bar quietly.
 *
 * TO DISABLE (for standard phones like Samsung/Pixel):
 *   You may not need this — the BroadcastReceiver alone is enough.
 *   Only enable this for OEM phones with aggressive battery killers.
 */
class CallSyncService : Service() {

    companion object {
        private const val CHANNEL_ID   = "call_sync_channel"
        private const val CHANNEL_NAME = "Call Sync"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY → system restarts service if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null  // Not a bound service

    // ─────────────────────────────────────────────────────────────────────
    //  Notification setup
    // ─────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW    // Silent — no sound or badge
        ).apply {
            description = "Tracks call activity for CRM sync"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NotifyTechAI")
            .setContentText("Call sync active")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)      // Cannot be dismissed by swipe
            .build()
    }
}
