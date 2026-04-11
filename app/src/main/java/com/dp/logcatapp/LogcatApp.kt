package com.dp.logcatapp

import android.app.Application
import com.dp.logcatapp.util.CrashHandler
import com.dp.logger.Logger

@Suppress("unused")
class LogcatApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Logger.init("LogcatReader")
    CrashHandler.install(this)
    CrashHandler.createNotificationChannel(this)
  }
}
