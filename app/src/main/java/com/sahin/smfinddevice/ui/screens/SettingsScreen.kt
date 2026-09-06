package com.sahin.smfinddevice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sahin.smfinddevice.storage.SecureConfigStore

/**
 * Settings & Information hub. Each row here either shows real live status (App Protection)
 * or navigates to a dedicated screen — nothing here is a dead button.
 */
@Composable
fun SettingsScreen(
    onOpenPermissions: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore.getInstance(context) }
    var protectionEnabled by remember { mutableStateOf(store.protectionEnabled) }
    var showDisableConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Manage your protection",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        SettingsRow(Icons.Default.Lock, "Permissions", "View & manage", onClick = onOpenPermissions)
        SettingsRow(
            Icons.Default.Shield,
            "App Protection",
            if (protectionEnabled) "Active — Device Admin not required" else "Disabled",
            onClick = onOpenPrivacy
        )
        SettingsRow(Icons.Default.BatteryChargingFull, "Battery Optimization", "Guidance & exemptions", onClick = onOpenBattery)
        SettingsRow(Icons.Default.Save, "Backup & Restore", "Export or import settings", onClick = onOpenBackup)
        SettingsRow(Icons.Default.Info, "About", "Version, developer", onClick = onOpenAbout)
        SettingsRow(Icons.Default.Help, "Help & FAQ", "Troubleshooting", onClick = onOpenHelp)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(
            "When disabled, no incoming SMS will be treated as a recovery command, and no " +
                "location will ever be acquired or sent automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (protectionEnabled) showDisableConfirm = true
                else {
                    store.protectionEnabled = true
                    protectionEnabled = true
                }
            },
            colors = if (protectionEnabled) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (protectionEnabled) "Deactivate Protection" else "Activate Protection")
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("Disable protection?") },
            text = { Text("Your device will no longer respond to any recovery SMS until you re-enable protection.") },
            confirmButton = {
                TextButton(onClick = {
                    store.protectionEnabled = false
                    protectionEnabled = false
                    showDisableConfirm = false
                }) { Text("Disable") }
            },
            dismissButton = { TextButton(onClick = { showDisableConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
