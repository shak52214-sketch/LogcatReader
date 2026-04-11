package com.dp.logcatapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dp.logger.Logger
import com.dp.logcatapp.activities.CrashActivity

@Suppress("unused")
class LogcatApp : Application() {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        Logger.init("LogcatReader")
        createCrashNotificationChannel()
        setupCrashHandler()
    }

    private fun createCrashNotificationChannel() {
        val channel = NotificationChannel(
            CRASH_CHANNEL_ID,
            "Crash Reports",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications shown when the app crashes"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun setupCrashHandler() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashLog = buildCrashLog(thread, throwable)
                saveCrashLog(crashLog)
                postCrashNotification(crashLog)
                launchCrashActivity(crashLog)
                Thread.sleep(800)
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                    ?: android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()
        sb.appendLine("=== App Crash Report ===")
        sb.appendLine("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine("Thread: ${thread.name}")
        sb.appendLine("App: ${packageName}")
        sb.appendLine()
        sb.appendLine("--- Exception ---")
        sb.appendLine(throwable.toString())
        sb.appendLine()
        sb.appendLine("--- Stack Trace ---")
        sb.appendLine(throwable.stackTraceToString())
        var cause = throwable.cause
        while (cause != null) {
            sb.appendLine()
            sb.appendLine("--- Caused by ---")
            sb.appendLine(cause.stackTraceToString())
            cause = cause.cause
        }
        return sb.toString()
    }

    private fun saveCrashLog(crashLog: String) {
        try {
            getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(CRASH_LOG_KEY, crashLog)
                .putBoolean(HAS_CRASH_KEY, true)
                .apply()
        } catch (_: Exception) {}
    }

    private fun postCrashNotification(crashLog: String) {
        try {
            val copyIntent = Intent(this, CopyClipboardReceiver::class.java).apply {
                putExtra(CopyClipboardReceiver.EXTRA_TEXT, crashLog)
                putExtra(CopyClipboardReceiver.EXTRA_LABEL, "Crash Log")
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                this, 0, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val viewIntent = Intent(this, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_LOG, crashLog)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val viewPendingIntent = PendingIntent.getActivity(
                this, 1, viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val shortSummary = crashLog.lines().firstOrNull { it.startsWith("---") || it.contains("Exception") }
                ?: crashLog.take(200)

            val notification = NotificationCompat.Builder(this, CRASH_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("App Crashed")
                .setContentText(shortSummary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(shortSummary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(viewPendingIntent)
                .addAction(android.R.drawable.ic_menu_edit, "Copy Log", copyPendingIntent)
                .addAction(android.R.drawable.ic_menu_view, "View Details", viewPendingIntent)
                .build()

            NotificationManagerCompat.from(this).notify(CRASH_NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun launchCrashActivity(crashLog: String) {
        try {
            val intent = Intent(this, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_LOG, crashLog)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                )
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    companion object {
        const val CRASH_CHANNEL_ID = "crash_reports_channel"
        const val CRASH_NOTIFICATION_ID = 9999
        const val CRASH_PREFS = "crash_prefs"
        const val CRASH_LOG_KEY = "last_crash_log"
        const val HAS_CRASH_KEY = "has_crash"
    }
}
