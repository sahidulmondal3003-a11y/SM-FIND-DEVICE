package com.sahin.smfinddevice.ui.screens.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahin.smfinddevice.data.ActivityCategory
import com.sahin.smfinddevice.data.ActivityEvent
import com.sahin.smfinddevice.data.ActivityStatus
import com.sahin.smfinddevice.data.RecoveryNumber
import com.sahin.smfinddevice.data.defaultRecoveryCommands
import com.sahin.smfinddevice.permissions.PermissionChecker
import com.sahin.smfinddevice.security.PhoneNumberNormalizer
import com.sahin.smfinddevice.storage.SecureConfigStore
import java.util.UUID

private enum class SetupStep { WELCOME, PERMISSIONS_EXPLAIN, LOCATION_PERMISSION, SMS_PERMISSION, NOTIFICATIONS, RECOVERY_NUMBER, COMMANDS, ACTIVATE }

private val ALL_STEPS = SetupStep.entries

@Composable
fun SetupWizardScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore.getInstance(context) }
    val checker = remember { PermissionChecker(context) }
    var step by remember { mutableStateOf(SetupStep.WELCOME) }
    var recoveryInput by remember { mutableStateOf("") }
    var labelInput by remember { mutableStateOf("Primary") }
    var addError by remember { mutableStateOf<String?>(null) }

    fun logPermissionEvent(label: String) {
        store.appendActivityEvent(
            ActivityEvent(
                id = UUID.randomUUID().toString(),
                category = ActivityCategory.PERMISSION,
                title = "Permission Reviewed",
                detail = label,
                timestampMillis = System.currentTimeMillis(),
                status = ActivityStatus.INFO
            )
        )
    }

    val locationPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        logPermissionEvent("Location permission")
        step = SetupStep.SMS_PERMISSION
    }
    val smsPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        logPermissionEvent("SMS permission")
        step = SetupStep.NOTIFICATIONS
    }
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        logPermissionEvent("Notification permission")
        step = SetupStep.RECOVERY_NUMBER
    }

    val stepIndex = ALL_STEPS.indexOf(step)
    val progress by animateFloatAsState(
        targetValue = (stepIndex + 1f) / ALL_STEPS.size,
        animationSpec = tween(350),
        label = "setup_progress"
    )

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (step != SetupStep.WELCOME) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        "Setup Wizard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Step ${stepIndex + 1} of ${ALL_STEPS.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                when (step) {
                    SetupStep.WELCOME -> {
                        Spacer(Modifier.height(40.dp))
                        SetupHeroIcon(Icons.Default.Security)
                        Spacer(Modifier.height(20.dp))
                        Text("SM Find Device", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Developed By Sahin", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "This app helps you recover your own lost Android device using an " +
                                "authorized SMS location request. Setup takes about two minutes."
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { step = SetupStep.PERMISSIONS_EXPLAIN }, modifier = Modifier.fillMaxWidth()) {
                            Text("Get Started")
                        }
                    }

                    SetupStep.PERMISSIONS_EXPLAIN -> {
                        SetupHeroIcon(Icons.Default.Security)
                        StepTitle("Permissions")
                        Text(
                            "We need the following permissions to help you recover your device when it's lost.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        PermissionPreviewCard(Icons.Default.LocationOn, "Location (Precise)", "Required for accurate location")
                        PermissionPreviewCard(Icons.Default.Sms, "SMS Access", "Required for recovery SMS")
                        PermissionPreviewCard(Icons.Default.Notifications, "Notifications", "Required for alerts & status")
                        PermissionPreviewCard(Icons.Default.Sync, "Background Activity", "Required for reliable operation")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Each is explained before it's requested, and each is used only for the recovery " +
                                "function you're setting up — never for background tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { step = SetupStep.LOCATION_PERMISSION }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    }

                    SetupStep.LOCATION_PERMISSION -> {
                        SetupHeroIcon(Icons.Default.LocationOn)
                        StepTitle("Location Permission")
                        Text(
                            "Precise location lets SM Find Device report your device's real position " +
                                "when you send an authorized recovery SMS. If you deny this, only an " +
                                "approximate location — or none — will be available, and the app will " +
                                "say so honestly rather than claim otherwise."
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                locationPermLauncher.launch(perms.toTypedArray())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Grant Location Permission") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { step = SetupStep.SMS_PERMISSION }) { Text("Skip for now") }
                    }

                    SetupStep.SMS_PERMISSION -> {
                        SetupHeroIcon(Icons.Default.Sms)
                        StepTitle("SMS Permission")
                        Text("SMS access is used to recognize authorized recovery commands and return your device location.")
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { smsPermLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Grant SMS Permission") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { step = SetupStep.NOTIFICATIONS }) { Text("Skip for now") }
                    }

                    SetupStep.NOTIFICATIONS -> {
                        SetupHeroIcon(Icons.Default.Notifications)
                        StepTitle("Notifications")
                        Text("Notifications tell you about protection status and active recovery operations.")
                        Spacer(Modifier.height(24.dp))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Grant Notification Permission")
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { step = SetupStep.RECOVERY_NUMBER }) { Text("Skip for now") }
                        } else {
                            Button(onClick = { step = SetupStep.RECOVERY_NUMBER }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                        }
                    }

                    SetupStep.RECOVERY_NUMBER -> {
                        StepTitle("Authorized Recovery Number")
                        Text("Add at least one trusted number that will be allowed to request this device's location. You can add more later.")
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = recoveryInput,
                            onValueChange = { recoveryInput = it },
                            label = { Text("Phone number") },
                            placeholder = { Text("+91XXXXXXXXXX") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = labelInput,
                            onValueChange = { labelInput = it },
                            label = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        addError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val normalizer = PhoneNumberNormalizer()
                                val normalized = normalizer.normalize(recoveryInput)
                                if (normalized == null) {
                                    addError = "Enter a valid phone number."
                                } else {
                                    val result = store.addRecoveryNumber(
                                        RecoveryNumber(e164Number = normalized, label = labelInput.ifBlank { "Primary" }, verified = true)
                                    )
                                    result.onSuccess { step = SetupStep.COMMANDS }
                                        .onFailure { addError = it.message }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save and Continue") }
                    }

                    SetupStep.COMMANDS -> {
                        StepTitle("SMS Commands")
                        Text("15 recovery command slots are ready with sensible defaults. You can rename or disable any of them later from Authorized Setup.")
                        Spacer(Modifier.height(12.dp))
                        defaultRecoveryCommands().take(5).forEach {
                            Text("• ${it.text}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("...and 10 more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                store.writeCommands(defaultRecoveryCommands())
                                step = SetupStep.ACTIVATE
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Accept Default Commands") }
                    }

                    SetupStep.ACTIVATE -> {
                        SetupHeroIcon(Icons.Default.CheckCircle)
                        StepTitle("Activate Protection")
                        Text(
                            "Only your authorized recovery numbers can request your device location. " +
                                "Never share your recovery number or recovery configuration with untrusted people."
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                store.protectionEnabled = true
                                onFinished()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Activate Protection") }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SetupHeroIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PermissionPreviewCard(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
