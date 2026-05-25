package com.notifytechai.callsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BootReceiver — re-starts the sync service after device reboot.
 *
 * WHY THIS IS NEEDED:
 * ───────────────────
 * BroadcastReceivers declared in the manifest survive reboots
 * automatically for most intents. However, on many OEM devices
 * (Xiaomi, Oppo, Vivo, etc.) apps are killed on reboot and the
 * PHONE_STATE receiver stops firing.
 *
 * This receiver wakes up after boot and starts CallSyncService
 * as a foreground service, which keeps the app alive reliably.
 *
 * Requires:
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted — starting CallSyncService")

            val serviceIntent = Intent(context, CallSyncService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
