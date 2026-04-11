package com.dp.logcatapp

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class CopyClipboardReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Copied"
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_LABEL = "extra_label"
    }
}
