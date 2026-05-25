/**
 * NotifyTechAI — Apps Script Webhook
 * ────────────────────────────────────
 * Deploy this as a Web App:
 *   Deploy → New Deployment → Web App
 *   Execute As: Me
 *   Access: Anyone
 *
 * Replace YOUR_SHEET_ID with your Google Sheet ID.
 * Sheet must have a tab named "Calls".
 */

const SHEET_ID   = "YOUR_SHEET_ID";
const SHEET_NAME = "Calls";

/**
 * doPost — receives call data from the Android app.
 *
 * Expected JSON body:
 * {
 *   "agent":     "Rahul",
 *   "number":    "+919876543210",
 *   "duration":  "142",           ← seconds
 *   "type":      "OUTGOING",
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 */
function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);

    const sheet = SpreadsheetApp
      .openById(SHEET_ID)
      .getSheetByName(SHEET_NAME);

    // Format duration from seconds to mm:ss
    const secs     = parseInt(data.duration) || 0;
    const mins     = Math.floor(secs / 60);
    const remSecs  = secs % 60;
    const formatted = `${mins}m ${remSecs}s`;

    sheet.appendRow([
      new Date(),                            // Timestamp (server time)
      data.timestamp || "",                  // Timestamp (device time)
      data.agent     || "Unknown",           // Agent name
      data.number    || "",                  // Phone number
      data.duration  || "0",                // Duration (raw seconds)
      formatted,                             // Duration (human readable)
      data.type      || "UNKNOWN"            // OUTGOING / INCOMING / MISSED / TEST
    ]);

    return ContentService
      .createTextOutput(JSON.stringify({ success: true }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ success: false, error: err.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * doGet — health check endpoint.
 * Visit the URL in a browser to confirm the webhook is live.
 */
function doGet() {
  return ContentService
    .createTextOutput(JSON.stringify({ status: "NotifyTechAI webhook is live ✅" }))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * setupSheet — run this ONCE manually to create the header row.
 * Tools → Run → setupSheet
 */
function setupSheet() {
  const ss    = SpreadsheetApp.openById(SHEET_ID);
  let sheet   = ss.getSheetByName(SHEET_NAME);

  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
  }

  // Only add headers if the sheet is empty
  if (sheet.getLastRow() === 0) {
    sheet.appendRow([
      "Server Time",
      "Device Time",
      "Agent",
      "Phone Number",
      "Duration (s)",
      "Duration",
      "Call Type"
    ]);

    // Style the header row
    sheet.getRange(1, 1, 1, 7)
      .setFontWeight("bold")
      .setBackground("#1A1A2E")
      .setFontColor("#FFFFFF");
  }

  Logger.log("Sheet setup complete.");
}
