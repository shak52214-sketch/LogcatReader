package com.dp.logcatapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dp.logcatapp.util.ShizukuPermissionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionStatusScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shizukuState by remember { mutableStateOf(ShizukuPermissionManager.shizukuState.value) }
    var isGranting by remember { mutableStateOf(false) }
    var grantMessage by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        ShizukuPermissionManager.shizukuState.collectLatest { state ->
            shizukuState = state
        }
    }

    val permissionsToCheck = remember {
        buildList {
            add(PermissionEntry("READ_LOGS", Manifest.permission.READ_LOGS, critical = true))
            add(PermissionEntry("POST_NOTIFICATIONS", Manifest.permission.POST_NOTIFICATIONS))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(PermissionEntry("READ_EXTERNAL_STORAGE", Manifest.permission.READ_EXTERNAL_STORAGE))
                add(PermissionEntry("WRITE_EXTERNAL_STORAGE", Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
            add(PermissionEntry("FOREGROUND_SERVICE", Manifest.permission.FOREGROUND_SERVICE))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission & Status") },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Shizuku Status")

            ShizukuStatusCard(
                state = shizukuState,
                onRequestPermission = {
                    ShizukuPermissionManager.requestShizukuPermissionIfNeeded()
                }
            )

            SectionTitle("Grant READ_LOGS via Shizuku")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Use Shizuku to automatically grant android.permission.READ_LOGS without ADB.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AnimatedVisibility(
                        visible = grantMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        grantMessage?.let { msg ->
                            Surface(
                                color = if (msg.startsWith("Success"))
                                    Color(0xFF1B5E20).copy(alpha = 0.15f)
                                else
                                    Color(0xFFB71C1C).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = msg,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (msg.startsWith("Success"))
                                        Color(0xFF1B5E20)
                                    else
                                        Color(0xFFB71C1C),
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                isGranting = true
                                grantMessage = null
                                val result = ShizukuPermissionManager.grantReadLogsPermission(context)
                                isGranting = false
                                grantMessage = when (result) {
                                    ShizukuPermissionManager.GrantResult.GRANTED ->
                                        "Success: READ_LOGS permission granted. Please restart the app."
                                    ShizukuPermissionManager.GrantResult.DENIED ->
                                        "Denied: Shizuku permission was denied."
                                    ShizukuPermissionManager.GrantResult.FAILED ->
                                        "Failed: Could not grant permission via Shizuku."
                                    ShizukuPermissionManager.GrantResult.PERMISSION_REQUESTED ->
                                        "Shizuku permission requested. Please accept the prompt and try again."
                                    ShizukuPermissionManager.GrantResult.SHIZUKU_NOT_INSTALLED ->
                                        "Shizuku is not installed. Install it from Play Store or GitHub."
                                    ShizukuPermissionManager.GrantResult.SHIZUKU_NOT_RUNNING ->
                                        "Shizuku is not running. Start Shizuku and try again."
                                }
                                refreshTick++
                            }
                        },
                        enabled = !isGranting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isGranting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Grant READ_LOGS via Shizuku")
                    }
                }
            }

            SectionTitle("Permission Status")

            permissionsToCheck.forEach { entry ->
                val granted = remember(refreshTick) {
                    ContextCompat.checkSelfPermission(
                        context,
                        entry.permission,
                    ) == PackageManager.PERMISSION_GRANTED
                }
                PermissionStatusRow(
                    name = entry.name,
                    granted = granted,
                    critical = entry.critical,
                )
            }

            SectionTitle("Connectivity Status")

            val connectivityStatus = remember(refreshTick) { getConnectivityStatus(context) }

            connectivityStatus.forEach { (label, value) ->
                ConnectivityRow(label = label, value = value)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ShizukuStatusCard(
    state: ShizukuPermissionManager.ShizukuState,
    onRequestPermission: () -> Unit,
) {
    val (icon, color, title, description) = when (state) {
        ShizukuPermissionManager.ShizukuState.READY -> StatusDisplay(
            icon = Icons.Default.Check,
            color = Color(0xFF2E7D32),
            title = "Shizuku: Ready",
            description = "Connected and permission granted.",
        )
        ShizukuPermissionManager.ShizukuState.PERMISSION_NEEDED -> StatusDisplay(
            icon = Icons.Default.Warning,
            color = Color(0xFFF57F17),
            title = "Shizuku: Permission Needed",
            description = "Shizuku is running but needs permission to be granted.",
        )
        ShizukuPermissionManager.ShizukuState.NOT_RUNNING -> StatusDisplay(
            icon = Icons.Default.Close,
            color = Color(0xFFC62828),
            title = "Shizuku: Not Running",
            description = "Shizuku is installed but not running. Start it from the Shizuku app.",
        )
        ShizukuPermissionManager.ShizukuState.NOT_INSTALLED -> StatusDisplay(
            icon = Icons.Default.Info,
            color = Color(0xFF1565C0),
            title = "Shizuku: Not Installed",
            description = "Install Shizuku from Play Store or GitHub to use this feature.",
        )
        ShizukuPermissionManager.ShizukuState.UNKNOWN -> StatusDisplay(
            icon = Icons.Default.Info,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            title = "Shizuku: Checking...",
            description = "Detecting Shizuku status.",
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (state == ShizukuPermissionManager.ShizukuState.PERMISSION_NEEDED) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
                ) {
                    Text("Grant Shizuku Permission")
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    granted: Boolean,
    critical: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                Color(0xFF2E7D32).copy(alpha = 0.08f)
            else if (critical)
                Color(0xFFC62828).copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (critical) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (critical && !granted) {
                    Text(
                        text = "Required for logcat reading",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = if (granted) "Granted" else "Denied",
                    tint = if (granted) Color(0xFF2E7D32) else if (critical) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (granted) "Granted" else "Denied",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (granted) Color(0xFF2E7D32) else if (critical) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConnectivityRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (value == "Connected" || value == "Available")
                    Color(0xFF2E7D32)
                else if (value == "Not Connected" || value == "Unavailable")
                    Color(0xFFC62828)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class StatusDisplay(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val description: String,
)

private data class PermissionEntry(
    val name: String,
    val permission: String,
    val critical: Boolean = false,
)

private fun getConnectivityStatus(context: Context): List<Pair<String, String>> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return listOf("Network" to "Unknown")

    val network = cm.activeNetwork
    val caps = cm.getNetworkCapabilities(network)

    val result = mutableListOf<Pair<String, String>>()

    if (caps == null) {
        result.add("Network" to "Not Connected")
        result.add("WiFi" to "Unavailable")
        result.add("Mobile Data" to "Unavailable")
        result.add("Ethernet" to "Unavailable")
        result.add("VPN" to "Not Active")
        result.add("Internet" to "Unavailable")
        return result
    }

    result.add("Network" to "Connected")
    result.add("WiFi" to if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "Connected" else "Not Connected")
    result.add("Mobile Data" to if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) "Connected" else "Not Connected")
    result.add("Ethernet" to if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) "Connected" else "Not Connected")
    result.add("VPN" to if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) "Active" else "Not Active")
    result.add("Internet" to if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "Available" else "Unavailable")

    return result
}
