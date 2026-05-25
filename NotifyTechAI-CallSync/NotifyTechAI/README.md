# NotifyTechAI — Android Call Sync App

Automatically tracks SIM call duration from Android phones into your CRM via Google Sheets.

---

## Architecture

```
CRM (Apps Script)
       ↓
  Call Button (tel:)
       ↓
  Android SIM Call
       ↓
  [Call Ends] → CallReceiver (BroadcastReceiver)
       ↓
  Read Call Log (CallLog.Calls API)
       ↓
  SyncWorker (WorkManager) → retry on fail
       ↓
  Apps Script Webhook (doPost)
       ↓
  Google Sheet → CRM Analytics
```

---

## Project Structure

```
app/src/main/
 ├── java/com/notifytechai/callsync/
 │   ├── MainActivity.kt        — UI, permission handling, service starter
 │   ├── CallReceiver.kt        — Detects call state changes (IDLE = call ended)
 │   ├── ApiService.kt          — Retrofit interface for webhook
 │   ├── RetrofitClient.kt      — Singleton HTTP client (configure URL here)
 │   ├── CallRequest.kt         — JSON body data class
 │   ├── SyncWorker.kt          — WorkManager job: background sync with retry
 │   ├── CallSyncService.kt     — Foreground service (for OEM phone killers)
 │   └── BootReceiver.kt        — Re-starts service after reboot
 ├── res/layout/
 │   └── activity_main.xml      — Main screen UI
 ├── res/values/
 │   ├── strings.xml
 │   └── themes.xml
 └── AndroidManifest.xml        — Permissions + component declarations
```

---

## Setup Steps

### Step 1 — Configure Webhook URL

Open `RetrofitClient.kt` and replace:

```kotlin
private const val WEBHOOK_BASE_URL =
    "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID_HERE/"
```

With your Apps Script deployment URL. It must end with a trailing `/`.

---

### Step 2 — Deploy Apps Script Webhook

1. Go to [script.google.com](https://script.google.com)
2. Create a new project
3. Paste the webhook code (see `webhook/Code.gs`)
4. Deploy → New Deployment → Web App
5. Set **Execute As: Me**, **Access: Anyone**
6. Copy the URL and paste into `RetrofitClient.kt`

---

### Step 3 — Build & Install

```bash
# Open in Android Studio → Run on device
# OR build APK:
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

---

### Step 4 — Grant Permissions

On first launch, tap **Check Permissions** and allow:
- **Call Logs** — read call history
- **Phone State** — detect when calls end
- **Notifications** — foreground service notification (Android 13+)

---

### Step 5 — Test

Tap **Send Test Ping** → check your Google Sheet for a TEST row within 30 seconds.

---

## Permissions Explained

| Permission | Why |
|---|---|
| `READ_CALL_LOG` | Read number, duration, and type after call ends |
| `READ_PHONE_STATE` | Detect IDLE state (= call ended) |
| `INTERNET` | Send data to Apps Script webhook |
| `RECEIVE_BOOT_COMPLETED` | Re-start service after phone reboot |
| `FOREGROUND_SERVICE` | Keep sync alive on aggressive OEM phones |

---

## Recommended Test Phones

| Phone | Reliability |
|---|---|
| Google Pixel | ✅ Best |
| Samsung Galaxy | ✅ Very good |
| OnePlus | ⚠️ Disable battery optimization |
| Xiaomi / Realme / Oppo | ⚠️ Must whitelist app + enable AutoStart |

---

## Common Issues

**Calls not syncing on Chinese phones:**
- Settings → Battery → NotifyTechAI → No restrictions
- Settings → Apps → NotifyTechAI → Permissions → AutoStart → Enable

**Permission denied:**
- Settings → Apps → NotifyTechAI → Permissions → Enable all

**Webhook 302 redirect error:**
- Apps Script redirects GETs but POST bodies are lost on redirect
- Make sure your Retrofit POST follows redirects properly (OkHttp does by default for GET, not POST — configure manually if needed)

---

## Future Phases

| Phase | Features |
|---|---|
| Phase 2 | Login system, multi-agent auth, offline queue |
| Phase 3 | Next.js dashboard, PostgreSQL, charts, leaderboard |
| Phase 4 | AI call summary, voice transcription, real-time analytics |
