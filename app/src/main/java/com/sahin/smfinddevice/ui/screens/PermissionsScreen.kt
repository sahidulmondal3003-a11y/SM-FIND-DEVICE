package com.sahin.smfinddevice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sahin.smfinddevice.permissions.PermissionChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val checker = remember { PermissionChecker(context) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Permissions") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            PermissionRow("Precise location", checker.hasPreciseLocationPermission())
            PermissionRow("Approximate location", checker.hasApproximateLocationPermission())
            PermissionRow("Background location", checker.hasBackgroundLocationPermission())
            PermissionRow("Receive SMS", checker.hasReceiveSmsPermission())
            PermissionRow("Send SMS", checker.hasSendSmsPermission())
            PermissionRow("Notifications", checker.hasNotificationPermission())

            Spacer(Modifier.height(16.dp))
            Text(
                "If a permission shows as not granted, open your device Settings > Apps > " +
                    "SM Find Device > Permissions to grant it. Denied permissions limit recovery " +
                    "capability but never cause the app to fake success.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(12.dp))
        Text(
            if (granted) "Granted" else "Not granted",
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = if (granted) com.sahin.smfinddevice.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
    HorizontalDivider()
}
