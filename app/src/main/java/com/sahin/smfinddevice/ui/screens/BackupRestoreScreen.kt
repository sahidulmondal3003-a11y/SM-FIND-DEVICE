package com.sahin.smfinddevice.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahin.smfinddevice.data.RecoveryCommand
import com.sahin.smfinddevice.data.RecoveryNumber
import com.sahin.smfinddevice.storage.SecureConfigStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
private data class ConfigBackup(
    val recoveryNumbers: List<RecoveryNumber>,
    val commands: List<RecoveryCommand>,
    val protectionEnabled: Boolean
)

/**
 * Export/import of the real, on-device configuration (recovery numbers + commands +
 * protection toggle) — no new backend, no cloud upload; export goes through the standard
 * Android share sheet so the owner decides where the file goes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore.getInstance(context) }
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    var importText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Backup & Restore") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(
                "Export your recovery numbers and SMS commands as a text backup, or restore " +
                    "them on this device from a previous export. This stays entirely on your " +
                    "device and whichever app you choose to share it through — nothing is " +
                    "uploaded automatically.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(20.dp))
            Text("Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val backup = ConfigBackup(
                        recoveryNumbers = store.recoveryNumbers.value,
                        commands = store.readCommands(),
                        protectionEnabled = store.protectionEnabled
                    )
                    val text = json.encodeToString(backup)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "SM Find Device backup")
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Save backup"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export Configuration") }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("Paste backup text") },
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    try {
                        val backup = json.decodeFromString<ConfigBackup>(importText)
                        backup.recoveryNumbers.forEach { store.addRecoveryNumber(it) }
                        store.writeCommands(backup.commands)
                        store.protectionEnabled = backup.protectionEnabled
                        message = "Configuration restored."
                    } catch (e: Exception) {
                        message = "That doesn't look like a valid SM Find Device backup."
                    }
                },
                enabled = importText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore Configuration") }

            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
