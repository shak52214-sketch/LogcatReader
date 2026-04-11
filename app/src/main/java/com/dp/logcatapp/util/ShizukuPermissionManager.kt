package com.dp.logcatapp.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.dp.logcatapp.IShizukuUserService
import com.dp.logcatapp.services.ShizukuUserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

object ShizukuPermissionManager {

    private const val SHIZUKU_REQUEST_CODE = 1001

    private val _shizukuState = MutableStateFlow(ShizukuState.UNKNOWN)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _shizukuState.value = checkShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _shizukuState.value = ShizukuState.NOT_RUNNING
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                _shizukuState.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ShizukuState.READY
                } else {
                    checkShizukuState()
                }
            }
        }

    fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        _shizukuState.value = checkShizukuState()
    }

    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    fun requestShizukuPermissionIfNeeded() {
        val state = checkShizukuState()
        _shizukuState.value = state
        if (state == ShizukuState.PERMISSION_NEEDED) {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        }
    }

    suspend fun grantReadLogsPermission(context: Context): GrantResult {
        val state = checkShizukuState()
        _shizukuState.value = state

        return when (state) {
            ShizukuState.READY -> runViaUserService(context)
            ShizukuState.PERMISSION_NEEDED -> {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                GrantResult.PERMISSION_REQUESTED
            }
            ShizukuState.NOT_INSTALLED -> GrantResult.SHIZUKU_NOT_INSTALLED
            ShizukuState.NOT_RUNNING -> GrantResult.SHIZUKU_NOT_RUNNING
            ShizukuState.UNKNOWN -> GrantResult.FAILED
        }
    }

    private suspend fun runViaUserService(context: Context): GrantResult =
        withContext(Dispatchers.IO) {
            val serviceArgs = Shizuku.UserServiceArgs(
                ComponentName(context, ShizukuUserService::class.java)
            )
                .daemon(false)
                .processNameSuffix("shizuku_service")
                .debuggable(false)
                .version(1)

            val result = withTimeoutOrNull(15_000L) {
                suspendCancellableCoroutine { cont ->
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                            try {
                                val service = IShizukuUserService.Stub.asInterface(binder)
                                val exitCode = service.grantPermission(
                                    context.packageName,
                                    Manifest.permission.READ_LOGS,
                                )
                                service.exit()
                                cont.resume(if (exitCode == 0) GrantResult.GRANTED else GrantResult.FAILED)
                            } catch (e: Exception) {
                                cont.resume(GrantResult.FAILED)
                            } finally {
                                try {
                                    Shizuku.unbindUserService(serviceArgs, this, true)
                                } catch (_: Exception) {}
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            if (cont.isActive) cont.resume(GrantResult.FAILED)
                        }
                    }

                    cont.invokeOnCancellation {
                        try {
                            Shizuku.unbindUserService(serviceArgs, connection, true)
                        } catch (_: Exception) {}
                    }

                    try {
                        Shizuku.bindUserService(serviceArgs, connection)
                    } catch (e: Exception) {
                        cont.resume(GrantResult.FAILED)
                    }
                }
            }

            result ?: GrantResult.FAILED
        }

    private fun checkShizukuState(): ShizukuState {
        return try {
            if (!Shizuku.pingBinder()) return ShizukuState.NOT_RUNNING
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.READY
            } else {
                ShizukuState.PERMISSION_NEEDED
            }
        } catch (e: IllegalStateException) {
            ShizukuState.NOT_INSTALLED
        } catch (e: Exception) {
            ShizukuState.UNKNOWN
        }
    }

    enum class ShizukuState {
        UNKNOWN,
        NOT_INSTALLED,
        NOT_RUNNING,
        PERMISSION_NEEDED,
        READY,
    }

    enum class GrantResult {
        GRANTED,
        DENIED,
        FAILED,
        PERMISSION_REQUESTED,
        SHIZUKU_NOT_INSTALLED,
        SHIZUKU_NOT_RUNNING,
    }
}
