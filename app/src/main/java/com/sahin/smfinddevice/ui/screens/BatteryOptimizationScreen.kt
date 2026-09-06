package com.sahin.smfinddevice.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahin.smfinddevice.ui.theme.AlertAmber
import com.sahin.smfinddevice.ui.theme.SuccessGreen

/**
 * Real battery-optimization status via [PowerManager.isIgnoringBatteryOptimizations] — this
 * screen never silently changes the setting; it only explains it and deep-links to the
 * system screen where the owner grants it themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isExempt by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(PowerManager::class.java)
                pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            } else {
                true // battery optimization concept doesn't exist below M
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Battery Optimization") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isExempt) SuccessGreen.copy(alpha = 0.12f) else AlertAmber.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isExempt) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isExempt) SuccessGreen else AlertAmber
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isExempt) "Optimized" else "Restricted",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (isExempt) "Android will not aggressively restrict this app in the background."
                            else "Android may delay or kill background work needed to answer a recovery SMS promptly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                "For SM Find Device to reliably respond to a recovery SMS even when the app " +
                    "hasn't been opened recently, Android needs to know it's allowed to run briefly " +
                    "in the background. This does not enable any continuous background tracking — " +
                    "location is only ever acquired in direct response to an authorized request.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            if (!isExempt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            openAppBatterySettings(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Request Exemption") }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(onClick = { openAppBatterySettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open App Battery Settings")
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Not available on some manufacturer skins (Xiaomi, Oppo, Vivo, etc.) unless you " +
                    "also enable \"Autostart\" or an equivalent setting from that manufacturer's own " +
                    "battery screen — this app can only open the standard Android screen.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openAppBatterySettings(context: android.content.Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Nothing further we can safely do; the guidance text remains on screen.
    }
}
