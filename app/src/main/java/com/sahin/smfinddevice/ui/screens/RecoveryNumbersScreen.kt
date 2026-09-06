package com.sahin.smfinddevice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahin.smfinddevice.data.RecoveryNumber
import com.sahin.smfinddevice.viewmodel.RecoveryNumbersViewModel

/** Standalone screen kept for backward compatibility; the main flow now uses [RecoveryNumbersContent] inside AuthorizedSetupScreen's tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryNumbersScreen(
    onBack: () -> Unit,
    viewModel: RecoveryNumbersViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recovery Numbers") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            RecoveryNumbersContent(viewModel = viewModel)
        }
    }
}

/**
 * The reusable content of the Numbers tab: warning banner, list/empty-state, and the
 * floating "Add Number" affordance. Hosted by [RecoveryNumbersScreen] on its own, or by
 * AuthorizedSetupScreen inside a shared Scaffold + TabRow.
 */
@Composable
fun RecoveryNumbersContent(
    viewModel: RecoveryNumbersViewModel = viewModel(),
    snackbarHostState: SnackbarHostState? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val localSnackbar = snackbarHostState ?: remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        (uiState.errorMessage ?: uiState.infoMessage)?.let {
            localSnackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Only your enabled, verified recovery numbers can request this device's " +
                    "location. Never share your recovery number or configuration with " +
                    "untrusted people.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            if (uiState.hasNoActiveNumber) {
                WarningBanner("No active recovery number is configured.")
            }

            if (uiState.numbers.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No recovery numbers yet. Tap \"Add Number\" to add your first " +
                            "trusted number — for example, your own second phone or a family member's.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.numbers, key = { it.id }) { number ->
                        RecoveryNumberCard(
                            number = number,
                            onToggleEnabled = { enabled -> viewModel.setEnabled(number.id, enabled) },
                            onSetPrimary = { viewModel.setPrimary(number.id) },
                            onVerify = { viewModel.sendVerification(number.id) },
                            onConfirmVerified = { viewModel.confirmVerified(number.id) },
                            onTest = { viewModel.testRecovery(number.id) },
                            onRename = { newLabel -> viewModel.updateLabel(number.id, newLabel) },
                            onDelete = { viewModel.removeNumber(number.id) }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add Number") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (showAddDialog) {
        AddRecoveryNumberDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { number, label ->
                viewModel.addNumber(number, label)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun WarningBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun RecoveryNumberCard(
    number: RecoveryNumber,
    onToggleEnabled: (Boolean) -> Unit,
    onSetPrimary: () -> Unit,
    onVerify: () -> Unit,
    onConfirmVerified: () -> Unit,
    onTest: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var renaming by remember { mutableStateOf(false) }
    var renameText by remember(number.id) { mutableStateOf(number.label) }
    var confirmDelete by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(number.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (number.isPrimary) {
                            Spacer(Modifier.width(6.dp))
                            AssistChip(onClick = {}, label = { Text("Primary") }, enabled = false)
                        }
                    }
                    Text(number.maskedDisplay(), style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { onSetPrimary() }) {
                    Icon(
                        if (number.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Set as primary recovery number",
                        tint = if (number.isPrimary) com.sahin.smfinddevice.ui.theme.AlertAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { renaming = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    label = if (number.verified) "Verified" else "Not verified",
                    positive = number.verified
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(
                    label = if (number.enabled) "Enabled" else "Disabled",
                    positive = number.enabled
                )
                Spacer(Modifier.weight(1f))
                Switch(checked = number.enabled, onCheckedChange = onToggleEnabled)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!number.verified) {
                    OutlinedButton(onClick = onVerify) { Text("Verify Number") }
                    TextButton(onClick = onConfirmVerified) { Text("I received it") }
                } else {
                    OutlinedButton(onClick = onTest, enabled = number.enabled) { Text("Test Recovery") }
                }
            }
        }
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Label") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { onRename(renameText); renaming = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove recovery number?") },
            text = { Text("${number.label} will no longer be able to request this device's location.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatusChip(label: String, positive: Boolean) {
    val container = if (positive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (positive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = container, shape = MaterialTheme.shapes.extraSmall) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (positive) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

@Composable
private fun AddRecoveryNumberDialog(
    onDismiss: () -> Unit,
    onConfirm: (number: String, label: String) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recovery Number") },
        text = {
            Column {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone number") },
                    placeholder = { Text("+91XXXXXXXXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    placeholder = { Text("Primary, Family, Emergency...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(number, label) },
                enabled = number.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
