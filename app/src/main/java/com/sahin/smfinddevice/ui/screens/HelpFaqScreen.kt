package com.sahin.smfinddevice.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class FaqItem(val question: String, val answer: String)

private val FAQ_ITEMS = listOf(
    FaqItem("GPS is disabled", "Turn on Location in your device's quick settings or Settings > Location. Without it, only a last-known or approximate position may be available."),
    FaqItem("Permission denied", "Open Settings > Apps > SM Find Device > Permissions and grant Location and SMS. The app never works around a denied permission."),
    FaqItem("SMS unavailable / no SIM", "Recovery SMS requires an active SIM with SMS service on this device. Without one, incoming commands can't be received and replies can't be sent."),
    FaqItem("Airplane mode", "Airplane mode disables both SMS and most location fixes. Recovery cannot function until it's turned off."),
    FaqItem("Battery restrictions", "Some manufacturers restrict background apps aggressively. See Battery Optimization in Settings for guidance specific to this device."),
    FaqItem("Background restrictions", "Android 12+ limits background location and services. This app requests only what's needed and explains each permission before asking."),
    FaqItem("Poor GPS signal", "Indoors or in dense areas, GPS accuracy drops. The app will report the real accuracy value rather than pretending it's precise."),
    FaqItem("Location services disabled system-wide", "If Location is off at the system level, no app — including this one — can obtain a fix. Enable it in system Settings.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFaqScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Help & FAQ") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
        }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(20.dp), modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Text("Common issues", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
            }
            items(FAQ_ITEMS) { item ->
                FaqRow(item)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FaqRow(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(item.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
