package com.dp.logcatapp.services

import com.dp.logcatapp.IShizukuUserService
import kotlin.system.exitProcess

class ShizukuUserService : IShizukuUserService.Stub() {

    override fun exit() {
        exitProcess(0)
    }

    override fun grantPermission(packageName: String, permission: String): Int {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("pm", "grant", packageName, permission))
            process.waitFor()
        } catch (e: Exception) {
            -1
        }
    }
}
