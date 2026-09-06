package com.sahin.smfinddevice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahin.smfinddevice.storage.SecureConfigStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore.getInstance(context) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App Protection & Privacy") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .fillMaxSize()
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (store.protectionEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (store.protectionEnabled) "Protection is currently ON" else "Protection is currently OFF",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))
            Section("Location access", "Location is acquired only when an authorized recovery SMS is received, or when you run Test Location from this app. There is no continuous or scheduled background tracking.")
            Section("SMS access", "SMS is used only to recognize the 15 configured recovery commands from your authorized numbers, and to reply with location results. Every other message is ignored.")
            Section("Background operation", "The app briefly starts a foreground service, visible to Android and to you via notification, only for the duration of a single recovery request.")
            Section("Recovery numbers", "Numbers you add are stored only on this device, in Android Keystore-encrypted storage, and are never uploaded anywhere.")
            Section("Secure local storage", "Recovery numbers, commands, and settings are stored using EncryptedSharedPreferences backed by the Android Keystore.")
            Section("Location transmission", "A location result is sent only by SMS, only to the one authorized number that requested it. There is no cloud upload, analytics SDK, or advertising SDK in this app.")
            Section("Disabling protection", "Turn off Protection from Settings at any time. Once off, incoming SMS is never treated as a recovery command and no location is ever acquired automatically.")
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column(Modifier.padding(bottom = 18.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
