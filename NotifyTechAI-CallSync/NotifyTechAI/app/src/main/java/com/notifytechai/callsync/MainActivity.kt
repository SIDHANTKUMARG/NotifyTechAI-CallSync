package com.notifytechai.callsync

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.notifytechai.callsync.databinding.ActivityMainBinding

/**
 * MainActivity — the launcher screen.
 *
 * RESPONSIBILITIES:
 * ─────────────────
 * 1. Request runtime permissions (READ_CALL_LOG, READ_PHONE_STATE)
 * 2. Start CallSyncService (foreground service for background reliability)
 * 3. Show sync status to the agent
 * 4. Display agent name and webhook connection status
 *
 * IMPORTANT:
 *   READ_CALL_LOG is a "dangerous" permission — user must grant it
 *   manually the first time. On Android 11+ the permission dialog
 *   may route through Settings. This screen handles all of that.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ── Required permissions ──────────────────────────────────────────────
    private val requiredPermissions = mutableListOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Permission launcher ───────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            updateStatus(
                "⚠️ Permissions denied. Call tracking disabled.",
                isError = true
            )
            Toast.makeText(
                this,
                "Grant Call Log & Phone permissions for sync to work.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkAndRequestPermissions()
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UI Setup
    // ─────────────────────────────────────────────────────────────────────

    private fun setupUI() {
        // Show device model as agent ID for MVP (replace with login in Phase 2)
        binding.tvAgentName.text = "Agent: ${Build.MODEL}"
        binding.tvWebhookUrl.text = "Webhook: ${getWebhookSummary()}"

        binding.btnCheckPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnTestSync.setOnClickListener {
            testWebhookConnection()
        }
    }

    private fun getWebhookSummary(): String {
        // Show truncated URL so agent knows it's configured
        val url = RetrofitClient::class.java
            .getDeclaredField("WEBHOOK_BASE_URL")
            .let {
                it.isAccessible = true
                // Just show a placeholder — field is in companion object
                "https://script.google.com/macros/s/…/exec"
            }
        return url
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Permissions
    // ─────────────────────────────────────────────────────────────────────

    private fun checkAndRequestPermissions() {
        val missing = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            onPermissionsGranted()
        } else {
            updateStatus("Requesting permissions…")
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun onPermissionsGranted() {
        updateStatus("✅ Permissions granted — call sync is active")
        startSyncService()
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Start foreground service
    // ─────────────────────────────────────────────────────────────────────

    private fun startSyncService() {
        val intent = Intent(this, CallSyncService::class.java)
        startForegroundService(intent)
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Test webhook connection
    // ─────────────────────────────────────────────────────────────────────

    private fun testWebhookConnection() {
        updateStatus("🔄 Sending test ping to webhook…")

        // Send a dummy OUTGOING call record to verify the webhook works
        SyncWorker.enqueue(
            context  = applicationContext,
            number   = "0000000000",
            duration = "0",
            type     = "TEST",
            agent    = Build.MODEL
        )

        updateStatus("✅ Test queued — check your Google Sheet in ~30s")
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Status display
    // ─────────────────────────────────────────────────────────────────────

    private fun updateStatus(message: String, isError: Boolean = false) {
        binding.tvStatus.text = message
        binding.tvStatus.setTextColor(
            if (isError)
                getColor(android.R.color.holo_red_light)
            else
                getColor(android.R.color.holo_green_dark)
        )
    }
}
