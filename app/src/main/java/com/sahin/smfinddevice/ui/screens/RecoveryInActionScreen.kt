package com.sahin.smfinddevice.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahin.smfinddevice.data.LocationOutcome
import com.sahin.smfinddevice.ui.theme.SignalBlue
import com.sahin.smfinddevice.ui.theme.SignalTeal
import com.sahin.smfinddevice.ui.theme.SuccessGreen
import com.sahin.smfinddevice.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Recovery In Action" — shown while Test Location is running and while showing its
 * result. Every value on screen comes from [DashboardViewModel]'s real, non-mocked
 * [LocationOutcome]; there is no fabricated coordinate or timestamp anywhere here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryInActionScreen(
    onBack: () -> Unit,
    commandLabel: String = "FIND MY DEVICE",
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recovery In Action") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                "Command matched: $commandLabel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (uiState.isRunningTest || uiState.lastTestOutcome == null) {
                    RadarPulse()
                } else {
                    ResultBadge(uiState.lastTestOutcome)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    uiState.isRunningTest -> "Acquiring high-accuracy location…"
                    uiState.lastTestOutcome == null -> "Ready to acquire location."
                    uiState.lastTestOutcome is LocationOutcome.Success -> "Location found"
                    uiState.lastTestOutcome is LocationOutcome.FallbackToLastKnown -> "Location found with limited accuracy"
                    else -> "Fresh location unavailable"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            when (val outcome = uiState.lastTestOutcome) {
                is LocationOutcome.Success -> LocationInfoCard(outcome.result.latitude, outcome.result.longitude, outcome.result.accuracyMeters, outcome.result.timestampMillis, isLastKnown = false) {
                    openMaps(context, outcome.result.mapsUrl())
                }
                is LocationOutcome.FallbackToLastKnown -> {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Last known location",
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LocationInfoCard(outcome.result.latitude, outcome.result.longitude, outcome.result.accuracyMeters, outcome.result.timestampMillis, isLastKnown = true) {
                        openMaps(context, outcome.result.mapsUrl())
                    }
                }
                is LocationOutcome.Failure -> {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(10.dp))
                            Text("Reason: ${outcome.reason}", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                null -> Unit
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.runTestLocation() },
                enabled = !uiState.isRunningTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.lastTestOutcome == null) "Acquire Location" else "Run Again")
            }
        }
    }
}

private fun openMaps(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // No maps-capable app available; the URL itself is still shown in the card.
    }
}

@Composable
private fun RadarPulse() {
    val transition = rememberInfiniteTransition(label = "radar")
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "ripple"
    )

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val maxRadius = size.minDimension / 2f
            for (i in 0..2) {
                val phase = ((ripple + i / 3f) % 1f)
                drawCircle(
                    color = SignalBlue.copy(alpha = (1f - phase) * 0.35f),
                    radius = maxRadius * phase,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SignalBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ResultBadge(outcome: LocationOutcome?) {
    val (icon, color) = when (outcome) {
        is LocationOutcome.Success -> Icons.Default.CheckCircle to SuccessGreen
        is LocationOutcome.FallbackToLastKnown -> Icons.Default.LocationOn to SignalTeal
        is LocationOutcome.Failure -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        null -> Icons.Default.LocationOn to SignalBlue
    }
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(56.dp))
    }
}

@Composable
private fun LocationInfoCard(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float,
    timestampMillis: Long,
    isLastKnown: Boolean,
    onOpenMaps: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            InfoRow("Latitude", "%.6f".format(latitude))
            InfoRow("Longitude", "%.6f".format(longitude))
            InfoRow("Accuracy", "${accuracyMeters.toInt()} m")
            InfoRow(if (isLastKnown) "Last updated" else "Time", dateFormat.format(Date(timestampMillis)))
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenMaps, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open in Google Maps")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
