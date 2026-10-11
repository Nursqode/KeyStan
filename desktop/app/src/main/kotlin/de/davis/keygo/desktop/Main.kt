/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.app.Screen
import de.davis.keygo.desktop.app.screen.Dialogs
import de.davis.keygo.desktop.app.screen.GeneratorScreen
import de.davis.keygo.desktop.app.screen.LockScreen
import de.davis.keygo.desktop.app.screen.SettingsScreen
import de.davis.keygo.desktop.app.screen.TransferScreen
import de.davis.keygo.desktop.app.screen.VaultScreen
import de.davis.keygo.desktop.app.sidebar.Sidebar
import de.davis.keygo.desktop.core.ui.theme.KeyGoTheme
import kotlinx.coroutines.delay

fun main() = application {
    val scope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(scope) }
    val state by viewModel.state.collectAsState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "KeyStan",
        state = rememberWindowState(size = DpSize(1320.dp, 860.dp)),
    ) {
        // Without a minimum the layout collapses into overlapping text rather than scrolling.
        LaunchedEffect(Unit) {
            window.minimumSize = java.awt.Dimension(1040, 680)
        }

        KeyGoTheme(darkTheme = state.darkTheme) {
            // One tick a second keeps the one-time codes live; when locked there is nothing to
            // count down, so the loop stops rather than waking the window for nothing.
            LaunchedEffect(state.locked) {
                while (!state.locked) {
                    delay(1_000)
                    viewModel.tick()
                }
            }
            Root(viewModel, state)
        }
    }
}

@Composable
private fun Root(viewModel: AppViewModel, state: AppState) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            if (state.locked) {
                LockScreen(viewModel, state)
            } else {
                Row(Modifier.fillMaxSize()) {
                    Sidebar(viewModel, state)
                    Box(Modifier.weight(1f)) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) {
                                when (state.screen) {
                                    Screen.VAULT -> VaultScreen(viewModel, state)
                                    Screen.GENERATOR -> GeneratorScreen(viewModel, state)
                                    Screen.SETTINGS -> SettingsScreen(viewModel, state)
                                    Screen.IMPORT -> TransferScreen(viewModel, state, isImport = true)
                                    Screen.EXPORT -> TransferScreen(viewModel, state, isImport = false)
                                }
                            }
                            state.status?.let { message ->
                                StatusBanner(message, state.strings.dismiss) { viewModel.clearStatus() }
                            }
                        }
                    }
                }
                Dialogs(viewModel, state)
            }

            if (state.busy) {
                BusyOverlay(state.busyLabel ?: state.strings.busyWorking)
            }
        }
    }
}

/** A transient message at the bottom of the window - "Copied", "Item deleted", and the like. */
@Composable
private fun StatusBanner(message: String, dismissLabel: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        delay(2_600)
        onDismiss()
    }
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/**
 * The overlay shown while the vault is being sealed or opened.
 *
 * Argon2id is deliberately slow, so the wait is visible by design; naming what is happening is the
 * difference between "slow app" and "strong encryption".
 */
@Composable
private fun BusyOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Brand mark used by the sidebar and the lock screen. */
@Composable
internal fun AppMark(modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size((size * 0.55).dp),
        )
    }
}

/** Icons for the sidebar's navigation entries, kept together so they stay consistent. */
internal object NavIcons {
    val Generator = Icons.Filled.TheaterComedy
    val Import = Icons.Filled.FileDownload
    val Export = Icons.Filled.FileUpload
    val Settings = Icons.Filled.Settings
    val Lock = Icons.Filled.Lock
    val Add = Icons.Filled.Add
}

/** Spacer used between sidebar sections. */
@Composable
internal fun SectionSpacer(height: Int = 8) = Spacer(Modifier.width(0.dp).size(0.dp, height.dp))
