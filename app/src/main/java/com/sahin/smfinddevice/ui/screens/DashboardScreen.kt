package com.sahin.smfinddevice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahin.smfinddevice.data.LocationOutcome
import com.sahin.smfinddevice.data.ProtectionState
import com.sahin.smfinddevice.data.ProtectionStateEvaluator
import com.sahin.smfinddevice.ui.theme.AlertAmber
import com.sahin.smfinddevice.ui.theme.DangerRed
import com.sahin.smfinddevice.ui.theme.NavyDeep
import com.sahin.smfinddevice.ui.theme.NavyMid
import com.sahin.smfinddevice.ui.theme.SignalBlue
import com.sahin.smfinddevice.ui.theme.SignalTeal
import com.sahin.smfinddevice.ui.theme.SuccessGreen
import com.sahin.smfinddevice.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onOpenRecoveryNumbers: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenRecoveryInAction: () -> Unit,
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            Spacer(Modifier.width(2.dp))
            BrandMark()
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f, fill = true)) {
                Text(
                    "SM Find Device",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Developed By Sahin",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onOpenActivity) {
                Icon(Icons.Default.Notifications, contentDescription = "Activity")
            }
        }

        Spacer(Modifier.height(18.dp))

        HeroProtectionCard(
            state = uiState.protectionState,
            onToggle = { viewModel.setProtectionEnabled(it) }
        )

        Spacer(Modifier.height(20.dp))

        Text("Device Recovery Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        RecoverySummaryCard(uiState, dateFormat)

        Spacer(Modifier.height(24.dp))

        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        QuickActionsGrid(
            isTestRunning = uiState.isRunningTest,
            activeNumbers = uiState.activeRecoveryNumberCount,
            enabledCommands = uiState.enabledCommandCount,
            onTestLocation = {
                viewModel.runTestLocation()
                onOpenRecoveryInAction()
            },
            onOpenRecoveryNumbers = onOpenRecoveryNumbers,
            onOpenCommands = onOpenCommands,
            onOpenActivity = onOpenActivity,
            onOpenSettings = onOpenSettings
        )

        Spacer(Modifier.height(24.dp))

        HealthCheckSection(checklist = uiState.checklist, onOpenPermissions = onOpenPermissions)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SignalBlue, SignalTeal))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HeroProtectionCard(state: ProtectionState, onToggle: (Boolean) -> Unit) {
    val (label, subtitle, gradient) = when (state) {
        ProtectionState.PROTECTION_ACTIVE -> Triple(
            "PROTECTION ACTIVE",
            "Your device is protected and ready to be recovered.",
            Brush.linearGradient(listOf(NavyDeep, NavyMid, SignalTeal.copy(alpha = 0.55f)))
        )
        ProtectionState.ACTION_REQUIRED -> Triple(
            "ACTION REQUIRED",
            "Finish setup — add a recovery number and enable a command.",
            Brush.linearGradient(listOf(Color(0xFF5C1210), DangerRed))
        )
        ProtectionState.LIMITED_PROTECTION -> Triple(
            "LIMITED PROTECTION",
            "Some capability is restricted by Android on this device.",
            Brush.linearGradient(listOf(Color(0xFF5C4200), AlertAmber))
        )
        ProtectionState.PROTECTION_DISABLED -> Triple(
            "PROTECTION DISABLED",
            "Recovery SMS will be ignored until you turn this on.",
            Brush.linearGradient(listOf(Color(0xFF33384A), Color(0xFF565D74)))
        )
    }

    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().background(gradient).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (state == ProtectionState.PROTECTION_ACTIVE) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = state != ProtectionState.PROTECTION_DISABLED,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}

@Composable
private fun RecoverySummaryCard(
    uiState: com.sahin.smfinddevice.viewmodel.DashboardUiState,
    dateFormat: SimpleDateFormat
) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            val location = uiState.lastKnownLocation
            if (location == null) {
                SummaryRow("Last Location", "No location requested yet")
            } else {
                SummaryRow("Last Location", "${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}")
                SummaryRow("Accuracy", "${location.accuracyMeters.toInt()} m")
                SummaryRow("Time", dateFormat.format(Date(location.timestampMillis)))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SummaryRow(
                "Last Recovery Request",
                if (uiState.lastRecoveryRequestAt > 0) dateFormat.format(Date(uiState.lastRecoveryRequestAt)) else "None yet"
            )
            SummaryRow(
                "Last Successful Test",
                if (uiState.lastSuccessfulTestAt > 0) dateFormat.format(Date(uiState.lastSuccessfulTestAt)) else "None yet",
                isLast = true
            )
        }
    }
}

/**
 * Label stacked above value so neither can collide with or clip the other, regardless of
 * screen width — long coordinates, timestamps, and request labels always have the full
 * card width to wrap into instead of competing side-by-side for space.
 */
@Composable
private fun SummaryRow(label: String, value: String, isLast: Boolean = false) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (!isLast) Spacer(Modifier.height(2.dp))
}

@Composable
private fun QuickActionsGrid(
    isTestRunning: Boolean,
    activeNumbers: Int,
    enabledCommands: Int,
    onTestLocation: () -> Unit,
    onOpenRecoveryNumbers: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.MyLocation,
                tint = SignalBlue,
                title = if (isTestRunning) "Testing…" else "Test Location",
                subtitle = "No SMS sent",
                loading = isTestRunning,
                onClick = onTestLocation
            )
            QuickActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.VerifiedUser,
                tint = SignalTeal,
                title = "Authorized Numbers",
                subtitle = "$activeNumbers active",
                onClick = onOpenRecoveryNumbers
            )
        }
        Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.Sms,
                tint = AlertAmber,
                title = "SMS Commands",
                subtitle = "$enabledCommands/15 enabled",
                onClick = onOpenCommands
            )
            QuickActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.ListAlt,
                tint = SuccessGreen,
                title = "Activity",
                subtitle = "Recent events",
                onClick = onOpenActivity
            )
        }
        Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.VerifiedUser,
                tint = MaterialTheme.colorScheme.primary,
                title = "Test Recovery",
                subtitle = "End-to-end test",
                onClick = onOpenRecoveryNumbers
            )
            QuickActionCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.Tune,
                tint = MaterialTheme.colorScheme.primary,
                title = "Settings",
                subtitle = "Privacy & more",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier
                .padding(14.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.small).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = tint)
                } else {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HealthCheckSection(
    checklist: ProtectionStateEvaluator.Checklist?,
    onOpenPermissions: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Run Health Check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Show") }
            }
            if (checklist != null) {
                val allOk = checklist.allRequiredMet && checklist.preciseLocation && checklist.backgroundCapability
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        if (allOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (allOk) SuccessGreen else AlertAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (allOk) "All systems are working properly" else "Some items need attention",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded && checklist != null) {
                Spacer(Modifier.height(10.dp))
                HealthRow("Location permission", checklist.locationPermission)
                HealthRow("Precise location", checklist.preciseLocation)
                HealthRow("Background capability", checklist.backgroundCapability)
                HealthRow("SMS capability", checklist.smsCapability)
                HealthRow("Recovery number configured", checklist.hasRecoveryNumber)
                HealthRow("Recovery command enabled", checklist.hasEnabledCommand)
                HealthRow("Protection enabled", checklist.ownerEnabledProtection)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenPermissions) { Text("Review Permissions") }
            }
        }
    }
}

@Composable
private fun HealthRow(label: String, ok: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (ok) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ok) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
