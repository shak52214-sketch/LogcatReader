package com.dp.logcatapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

object ShizukuPermissionManager {

    private const val SHIZUKU_REQUEST_CODE = 1001

    private val _shizukuState = MutableStateFlow(ShizukuState.UNKNOWN)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    private val _grantResult = MutableStateFlow<GrantResult?>(null)
    val grantResult: StateFlow<GrantResult?> = _grantResult.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _shizukuState.value = checkShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _shizukuState.value = ShizukuState.NOT_RUNNING
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    _shizukuState.value = ShizukuState.READY
                    _grantResult.value = GrantResult.GRANTED
                } else {
                    _grantResult.value = GrantResult.DENIED
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

        return withContext(Dispatchers.IO) {
            when (state) {
                ShizukuState.READY -> {
                    try {
                        val cmd = "pm grant ${context.packageName} ${Manifest.permission.READ_LOGS}"
                        val process: ShizukuRemoteProcess = Shizuku.newProcess(
                            arrayOf("sh", "-c", cmd),
                            null,
                            null
                        )
                        val exitCode = process.waitFor()
                        process.destroy()
                        if (exitCode == 0) {
                            GrantResult.GRANTED
                        } else {
                            GrantResult.FAILED
                        }
                    } catch (e: Exception) {
                        GrantResult.FAILED
                    }
                }
                ShizukuState.PERMISSION_NEEDED -> {
                    Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                    GrantResult.PERMISSION_REQUESTED
                }
                ShizukuState.NOT_INSTALLED -> GrantResult.SHIZUKU_NOT_INSTALLED
                ShizukuState.NOT_RUNNING -> GrantResult.SHIZUKU_NOT_RUNNING
                ShizukuState.UNKNOWN -> GrantResult.FAILED
            }
        }
    }

    private fun checkShizukuState(): ShizukuState {
        return try {
            if (!Shizuku.pingBinder()) {
                return ShizukuState.NOT_RUNNING
            }
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
