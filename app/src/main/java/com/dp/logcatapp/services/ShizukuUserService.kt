package com.dp.logcatapp.services

import android.os.IBinder
import com.dp.logcatapp.IShizukuUserService
import kotlin.system.exitProcess

class ShizukuUserService : IShizukuUserService.Stub() {

    override fun exit() {
        exitProcess(0)
    }

    override fun grantPermission(packageName: String, permission: String): Int {
        val viaIpm = runCatching { grantViaIPackageManager(packageName, permission) }.getOrElse { -1 }
        if (viaIpm == 0) return 0
        return grantViaShell(packageName, permission)
    }

    private fun grantViaIPackageManager(packageName: String, permission: String): Int {
        val smClass = Class.forName("android.os.ServiceManager")
        val binder = smClass.getMethod("getService", String::class.java)
            .invoke(null, "package") as? IBinder ?: return -1
        val stubClass = Class.forName("android.content.pm.IPackageManager\$Stub")
        val pm = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        val userId = android.os.UserHandle.myUserId()
        pm.javaClass.getMethod(
            "grantRuntimePermission",
            String::class.java,
            String::class.java,
            Int::class.java,
        ).invoke(pm, packageName, permission, userId)
        return 0
    }

    private fun grantViaShell(packageName: String, permission: String): Int {
        val pmPaths = listOf("/system/bin/pm", "/bin/pm", "pm")
        for (pmPath in pmPaths) {
            val result = runCatching {
                val p = Runtime.getRuntime().exec(arrayOf(pmPath, "grant", packageName, permission))
                p.inputStream.readBytes()
                p.errorStream.readBytes()
                val code = p.waitFor()
                p.destroy()
                code
            }.getOrNull()
            if (result == 0) return 0
        }
        return -1
    }
}
