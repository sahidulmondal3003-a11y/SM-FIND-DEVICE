package com.sahin.smfinddevice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sahin.smfinddevice.data.RecoveryCommand
import com.sahin.smfinddevice.data.defaultRecoveryCommands
import com.sahin.smfinddevice.storage.SecureConfigStore

/** Standalone screen kept for backward compatibility; the main flow now uses [SmsCommandsContent] inside AuthorizedSetupScreen's tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsCommandsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore.getInstance(context) }
    var commands by remember { mutableStateOf(store.readCommands()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Commands  (${commands.count { it.enabled }} / 15)") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            SmsCommandsContent(commands = commands, onCommandsChanged = { commands = it })
        }
    }
}

/** The reusable content of the Commands tab: the 15 slots plus reset-to-default. */
@Composable
fun SmsCommandsContent(
    commands: List<RecoveryCommand>,
    onCommandsChanged: (List<RecoveryCommand>) -> Unit
) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore.getInstance(context) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.weight(1f)) {
            items(commands, key = { it.slot }) { command ->
                CommandRow(
                    command = command,
                    onToggle = { enabled ->
                        val updated = commands.map { if (it.slot == command.slot) it.copy(enabled = enabled) else it }
                        store.writeCommands(updated)
                        onCommandsChanged(updated)
                    },
                    onEdit = { newText ->
                        val updated = commands.map { if (it.slot == command.slot) it.copy(text = newText) else it }
                        store.writeCommands(updated)
                        onCommandsChanged(updated)
                    }
                )
            }
        }
        OutlinedButton(
            onClick = {
                val updated = defaultRecoveryCommands()
                store.writeCommands(updated)
                onCommandsChanged(updated)
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) { Text("Reset to Default Commands") }
    }
}

@Composable
private fun CommandRow(command: RecoveryCommand, onToggle: (Boolean) -> Unit, onEdit: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var text by remember(command.slot) { mutableStateOf(command.text) }

    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(command.text, style = MaterialTheme.typography.bodyLarge)
                Text("Slot ${command.slot + 1}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { editing = true }) { Text("Edit") }
            Switch(checked = command.enabled, onCheckedChange = onToggle)
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit command") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { onEdit(text); editing = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
}
