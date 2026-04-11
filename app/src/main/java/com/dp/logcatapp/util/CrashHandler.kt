package com.dp.logcatapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dp.logcatapp.activities.CrashActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val log = buildCrashLog(thread, throwable)
            saveCrashLog(context, log)
            showCrashNotification(context, log)
            launchCrashActivity(context, log)
        } catch (_: Exception) {
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        const val CRASH_LOG_FILE = "last_crash.txt"
        const val CRASH_CHANNEL_ID = "crash_channel"
        const val CRASH_NOTIFICATION_ID = 9999
        const val EXTRA_CRASH_LOG = "crash_log"

        fun install(context: Context) {
            val existing = Thread.getDefaultUncaughtExceptionHandler()
            if (existing is CrashHandler) return
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context.applicationContext, existing)
            )
        }

        fun buildCrashLog(thread: Thread, throwable: Throwable): String {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            return buildString {
                appendLine("=== CRASH REPORT ===")
                appendLine("Time   : $time")
                appendLine("Thread : ${thread.name}")
                appendLine()
                appendLine(throwable.stackTraceToString())
            }
        }

        fun saveCrashLog(context: Context, log: String) {
            runCatching { File(context.filesDir, CRASH_LOG_FILE).writeText(log) }
        }

        fun loadLastCrash(context: Context): String? = runCatching {
            val f = File(context.filesDir, CRASH_LOG_FILE)
            if (f.exists()) f.readText() else null
        }.getOrNull()

        fun createNotificationChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CRASH_CHANNEL_ID,
                "Crash Logs",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Shows app crash details" }
            nm.createNotificationChannel(channel)
        }

        fun showCrashNotification(context: Context, crashLog: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val viewIntent = Intent(context, CrashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_CRASH_LOG, crashLog)
            }
            val viewPending = PendingIntent.getActivity(
                context, 0, viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val copyIntent = Intent(context, CrashCopyReceiver::class.java).apply {
                putExtra(EXTRA_CRASH_LOG, crashLog)
            }
            val copyPending = PendingIntent.getBroadcast(
                context, 1, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val firstLine = crashLog.lines().firstOrNull { it.isNotBlank() } ?: "Unknown error"
            val notification = androidx.core.app.NotificationCompat.Builder(context, CRASH_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Logcat Reader crashed")
                .setContentText(firstLine)
                .setStyle(
                    androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText(crashLog.take(1000))
                )
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(viewPending)
                .addAction(android.R.drawable.ic_menu_share, "Copy", copyPending)
                .build()

            nm.notify(CRASH_NOTIFICATION_ID, notification)
        }

        fun launchCrashActivity(context: Context, crashLog: String) {
            val intent = Intent(context, CrashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_CRASH_LOG, crashLog)
            }
            context.startActivity(intent)
        }
    }
}
