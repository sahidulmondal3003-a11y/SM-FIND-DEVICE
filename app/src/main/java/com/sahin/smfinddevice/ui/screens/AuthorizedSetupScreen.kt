package com.sahin.smfinddevice.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahin.smfinddevice.storage.SecureConfigStore
import com.sahin.smfinddevice.viewmodel.RecoveryNumbersViewModel

/**
 * "Authorized Numbers & SMS Commands" — a single screen with a Numbers / Commands
 * segmented tab layout, matching the design reference. Reuses the exact same
 * ViewModel + SecureConfigStore-backed logic as before; only the presentation changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizedSetupScreen(
    onBack: () -> Unit,
    numbersViewModel: RecoveryNumbersViewModel = viewModel()
) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore.getInstance(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var commands by remember { mutableStateOf(store.readCommands()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val enabledCount = commands.count { it.enabled }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Authorized Setup") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Numbers") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Commands ($enabledCount/15)") }
                    )
                }
                when (selectedTab) {
                    0 -> RecoveryNumbersContent(viewModel = numbersViewModel, snackbarHostState = snackbarHostState)
                    1 -> SmsCommandsContent(commands = commands, onCommandsChanged = { commands = it })
                }
            }
        }
    }
}
