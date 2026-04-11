<img src="/app/playstore_images/launcher_icon.png" width="192px" />

# Logcat Reader

A powerful, modern Android app for reading, filtering, recording, and analyzing device logs — with full Shizuku integration for zero-ADB permission setup.

## Most Important Code For Giving Permission
Shizuku.newProcess(
    arrayOf("sh", "-c", "pm grant your.package.name android.permission.READ_LOGS"),
    null,
    null
)

<a href="https://f-droid.org/packages/com.dp.logcatapp/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href='https://play.google.com/store/apps/details?id=com.dp.logcatapp'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height="80"/></a>

---

## Features

- **Real-time log streaming** — live logcat with zero lag
- **Advanced filtering** — filter by app, package, tag, message, PID, TID, priority, date, time, or any regex
- **Search with highlights** — inline search with regex support and hit navigation
- **Record & export** — record sessions to file, save, share, or export
- **Shizuku integration** — automatically grant `READ_LOGS` with zero ADB commands
- **Universal crash handler** — app crashes show a full-screen popup with the full stack trace and a Copy button, plus a persistent notification with Copy action
- **Permission & status screen** — live view of all permission states, Shizuku connectivity, and network status
- **Display options** — compact mode, font size, theme, dynamic color (Material You)
- **Dark / Light / Auto theme**

---

## Screenshots

<img src="/app/playstore_images/screenshots/dark_mode.png" width="300px" /> <img src="/app/playstore_images/screenshots/light_mode.png" width="300px" />
<img src="/app/playstore_images/screenshots/search.png" width="300px" /> <img src="/app/playstore_images/screenshots/compact_view.png" width="300px" />

---

## Permission Setup — READ_LOGS

The app needs `android.permission.READ_LOGS` to read system logs. This is a privileged permission and cannot be granted via a normal permission dialog. There are three ways to grant it:

---

### Method 1 — Shizuku (Recommended, No PC Required)

Shizuku lets the app grant itself `READ_LOGS` automatically, using the same `pm grant` command that ADB uses — no USB cable or PC needed.

#### Step 1: Install Shizuku

Download Shizuku from [Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub](https://github.com/RikkaApps/Shizuku/releases).

#### Step 2: Start Shizuku

Open Shizuku and start it using one of:

- **Wireless ADB** (Android 11+): Go to *Developer Options → Wireless debugging*, pair your phone with Shizuku — no PC needed.
- **USB ADB** (any version): Connect to a PC and run:
  ```sh
  adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh
  ```
- **Root**: If your device is rooted, Shizuku can start via root automatically.

#### Step 3: Grant Shizuku Permission to the App

When you open Logcat Reader, Shizuku will show a popup asking if it should allow this app access. Tap **Allow**.

#### Step 4: Grant READ_LOGS via the App

Open the app → tap **⋮ menu** → **Permission & Status**.

The screen will show Shizuku as **Ready** (green). Tap **Grant READ_LOGS via Shizuku**.

The app executes the following command in Shizuku's privileged shell:
```sh
pm grant com.dp.logcatapp android.permission.READ_LOGS
```

This is identical to running the same command via `adb shell` — Shizuku simply provides the privileged shell context without needing a PC. When the command succeeds, the status updates to **Granted** and a restart dialog appears.

#### Step 5: Restart

Tap **OK** in the restart dialog. After restart the app reads logs fully.

---

### Method 2 — ADB (PC Required)

Connect your device to a PC with USB debugging enabled, then run:

```sh
adb shell pm grant com.dp.logcatapp android.permission.READ_LOGS
```

Or the combined command that also force-stops the app (so the permission takes effect):
```sh
adb shell "pm grant com.dp.logcatapp android.permission.READ_LOGS && am force-stop com.dp.logcatapp"
```

Then reopen the app.

---

### Method 3 — Root

If your device is rooted, open the app → tap **⋮ menu** → **Permission & Status** (or wait for the permission dialog) → tap **Root Method**. The app will execute `pm grant` as root automatically.

---

## How Shizuku Integration Works (Technical)

Shizuku runs a persistent privileged server process as the `shell` user (same privilege level as `adb shell`). The app integrates with it in two ways:

### 1. Permission Request Flow

```
App init → Shizuku.addBinderReceivedListenerSticky()
         → Shizuku binder arrives → ShizukuState updated to READY / PERMISSION_NEEDED
         → If PERMISSION_NEEDED → Shizuku.requestPermission() → user dialog
         → If READY → auto-trigger grantReadLogsPermission()
```

### 2. READ_LOGS Grant — Primary Method (newProcess via Reflection)

Since `Shizuku.newProcess()` is private in Shizuku v13, the app accesses it via Java reflection:

```kotlin
val newProcess = Shizuku::class.java.getDeclaredMethod(
    "newProcess",
    Array<String>::class.java,
    Array<String>::class.java,
    String::class.java
)
newProcess.isAccessible = true
val process = newProcess.invoke(null,
    arrayOf("sh", "-c", "pm grant com.dp.logcatapp android.permission.READ_LOGS"),
    null, null
)
```

This runs the `pm grant` command inside Shizuku's privileged shell — exactly like `adb shell pm grant` but entirely on-device.

### 3. READ_LOGS Grant — Fallback Method (AIDL UserService)

If the reflection approach fails, the app falls back to a Shizuku UserService (AIDL):

1. App binds to `ShizukuUserService` via `Shizuku.bindUserService()`
2. The service runs inside Shizuku's privileged process
3. It tries `IPackageManager.grantRuntimePermission()` via reflection first
4. Falls back to `/system/bin/pm grant ...` shell command

### 4. Permission State Detection

The app detects `READ_LOGS` status using two checks in sequence:
1. `ContextCompat.checkSelfPermission()` — fast, reflects `pm grant` immediately
2. If that returns false, runs `logcat -d -t 2 -v uid` and checks if any log entry from a different UID is returned — this confirms actual READ access at the OS level

---

## Universal Crash Handler

The app installs a global `Thread.setDefaultUncaughtExceptionHandler` in `LogcatApp.onCreate()`. On any uncaught exception:

1. The full stack trace is captured and formatted
2. A **high-priority notification** is posted immediately with:
   - The crash summary in the notification body
   - A **"Copy Log"** action button that copies the full trace to clipboard
   - A **"View Details"** action that opens the crash screen
3. **CrashActivity** is launched — a full-screen overlay showing:
   - The complete stack trace in a scrollable monospace view
   - A **"Copy Log"** button
   - A **"Close App"** button
4. The original handler is called (or the process is killed)

---

## Building

### GitHub Actions (CI/CD)

Push to GitHub and the workflow in `.github/workflows/build.yml` triggers automatically:

- **Debug APK** — built on every push and pull request
- **Release APK (unsigned)** — built on pushes to `main`/`master` only

Both APKs are uploaded as artifacts (retained 30 days).

### Local Build

Requires Android SDK and JDK 17.

```sh
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
```

---

## Permissions

| Permission | Why it's needed |
|---|---|
| `READ_LOGS` | Read device logcat — must be granted via Shizuku, ADB, or root |
| `POST_NOTIFICATIONS` | Foreground service notification + crash report notifications |
| `ACCESS_NETWORK_STATE` | Connectivity status in the Permission & Status screen |
| `READ_EXTERNAL_STORAGE` | Open saved log files (Android < 10) |
| `WRITE_EXTERNAL_STORAGE` | Save log files (Android < 10) |
| `FOREGROUND_SERVICE` | Keep the logcat service running in the background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required on Android 14+ for foreground service |
| `QUERY_ALL_PACKAGES` | Resolve app names from PIDs in log entries |

---

## Architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Concurrency:** Coroutines + Flow
- **Database:** Room (filters, saved logs)
- **Preferences:** SharedPreferences / DataStore
- **Navigation:** Activity-based (no Compose Navigation)
- **Modules:** `:app`, `:logcat`, `:logger`, `:collections`, `:searchlogs`, `:util`, `:microbenchmark`

---

## Contributing

Pull requests are welcome! Please use [Square's code style](https://github.com/square/java-code-styles) for formatting. 🙏
