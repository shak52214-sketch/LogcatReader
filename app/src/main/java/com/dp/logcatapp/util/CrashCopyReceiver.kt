package com.dp.logcatapp.util

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

class CrashCopyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val log = intent.getStringExtra(CrashHandler.EXTRA_CRASH_LOG) ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Crash Log", log))
        context.showToast("Crash log copied to clipboard")
    }
}
