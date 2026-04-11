# Logcat Reader - Android App

## Overview

A native Android app (Kotlin + Jetpack Compose) for viewing, filtering, recording, and exporting logcat logs on Android devices.

**Package ID:** `com.dp.logcatapp`  
**Min SDK:** 23 (Android 6.0)  
**Target SDK:** 36

---

## Project Structure

```
app/                  # Main application module
  src/main/
    java/com/dp/logcatapp/
      activities/     # All Activity classes
      services/       # Foreground logcat service
      ui/
        screens/      # Compose screens
        common/       # Shared composables
        theme/        # Theme definitions
      util/           # Utility classes
    res/              # Resources (layouts, strings, drawables)
    AndroidManifest.xml

logcat/               # Logcat session module
logger/               # Logger module
collections/          # Collections utility module
searchlogs/           # Search logs module
util/                 # Shared utility module
microbenchmark/       # Benchmark module
```

---

## Key Features

- View device logcat in real-time
- Record, save, share and export logs
- Advanced filtering (app, tag, message, priority, pid, tid, date/time, regex)
- Shizuku integration for automatic READ_LOGS permission grant (auto-triggers when Shizuku is ready)
- Universal crash handler — shows crash log popup + sends notification with Copy button on any crash
- Permission & connectivity status screen with real logcat-based READ_LOGS detection
- Compact mode and display options
- Dark/light/auto theme

---

## Shizuku Integration

### How it works

The app integrates with [Shizuku](https://shizuku.rikka.app/) to automatically grant `android.permission.READ_LOGS` without requiring ADB or root.

**Flow:**
1. App initializes `ShizukuPermissionManager` on startup
2. User opens **Permission & Status** screen (via ⋮ menu → Permission & Status)
3. Screen shows Shizuku connection status and all permission statuses
4. User taps "Grant READ_LOGS via Shizuku" button
5. Shizuku executes `pm grant com.dp.logcatapp android.permission.READ_LOGS`
6. Restart the app for the permission to take effect

**Files:**
- `app/src/main/java/com/dp/logcatapp/util/ShizukuPermissionManager.kt` - Shizuku lifecycle and permission logic
- `app/src/main/java/com/dp/logcatapp/ui/screens/PermissionStatusScreen.kt` - Permission & connectivity status UI
- `app/src/main/java/com/dp/logcatapp/activities/PermissionStatusActivity.kt` - Activity hosting the status screen

**Dependencies:** `dev.rikka.shizuku:api:13.1.5` and `dev.rikka.shizuku:provider:13.1.5`

---

## Building

### GitHub Actions (CI/CD)

The project builds via GitHub Actions on every push/PR to main/master.

**Workflow:** `.github/workflows/build.yml`

- **Debug APK** - Built on all pushes and PRs
- **Release APK (unsigned)** - Built on pushes to main/master only

Artifacts are uploaded and retained for 30 days.

### Local Build

Requires Android SDK and JDK 17.

```sh
./gradlew assembleDebug   # Build debug APK
./gradlew assembleRelease # Build release APK
```

---

## Permissions

| Permission | Purpose |
|---|---|
| `READ_LOGS` | Required to read device logs (granted via Shizuku or ADB) |
| `POST_NOTIFICATIONS` | Foreground service notification + crash notifications |
| `ACCESS_NETWORK_STATE` | Connectivity status screen |
| `READ_EXTERNAL_STORAGE` | Read saved log files |
| `WRITE_EXTERNAL_STORAGE` | Save log files |
| `FOREGROUND_SERVICE` | Run logcat service in background |
| `QUERY_ALL_PACKAGES` | Show app names in log entries |

### Granting READ_LOGS

**Via Shizuku (recommended):** Use the built-in Permission & Status screen.

**Via ADB:**
```sh
adb shell pm grant com.dp.logcatapp android.permission.READ_LOGS
```

**Via Root:** The app has a built-in root method in the permission dialog.

---

## Architecture Notes

- Kotlin + Jetpack Compose (Material 3)
- Room database for filters and saved logs
- Coroutines for async operations
- Activity-based navigation (no Compose Navigation)
- Modular multi-module Gradle project
