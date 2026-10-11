/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.davis.keygo.desktop.app.AppDialog
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.app.DESKTOP_VERSION
import de.davis.keygo.desktop.app.platform.FileDialogs
import de.davis.keygo.desktop.core.ui.component.KeyGoCard
import de.davis.keygo.desktop.core.vault.data.ThemeMode

/** Preferences, the vault file's location, and what the file actually is. */
@Composable
fun SettingsScreen(viewModel: AppViewModel, state: AppState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                ThemeMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.settings.theme == mode,
                            onClick = { viewModel.updateSettings { it.copy(theme = mode) } },
                        )
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Follow the system"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                        )
                    }
                }
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Security", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Lock after ${state.settings.autoLockMinutes} minutes without input",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = state.settings.autoLockMinutes.toFloat(),
                    onValueChange = { minutes ->
                        viewModel.updateSettings { it.copy(autoLockMinutes = minutes.toInt()) }
                    },
                    valueRange = 0f..60f,
                )
                Text(
                    if (state.settings.autoLockMinutes == 0) "Automatic locking is off"
                    else "Idle time before the vault is sealed again",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = state.settings.concealSecrets,
                        onCheckedChange = { conceal ->
                            viewModel.updateSettings { it.copy(concealSecrets = conceal) }
                        },
                    )
                    Spacer(Modifier.size(12.dp))
                    Text("Hide passwords and card numbers until asked")
                }

                HorizontalDivider()
                Button(onClick = { viewModel.openDialog(AppDialog.CHANGE_PASSPHRASE) }) {
                    Text("Change the master passphrase")
                }
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Vault file", style = MaterialTheme.typography.titleMedium)
                Text(
                    state.vaultFileName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    viewModel.vaultFilePath().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            FileDialogs.open("Open a vault file", viewModel.vaultFilePath().parent)
                                ?.let(viewModel::useVaultFile)
                        },
                    ) {
                        Text("Open another file")
                    }
                }
                Text(
                    "Switching files locks the current vault first; the file on disk is left exactly " +
                        "as it was.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text(
                    "KeyStan Desktop $DESKTOP_VERSION · vault format v1",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Author: Nursqode", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "KeyStan is a fork of KeyGo by Davis Alessandru (OffRange), and the two share " +
                        "their vault format. Licensed under the GNU GPL v3.0 or later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "The vault file is the same sealed JSON backup the KeyGo Android app writes: " +
                        "Argon2id (64 MiB, 3 passes, 4 lanes) derives a key that AES-256-GCM-SIV uses " +
                        "to seal the payload. A file exported here restores on the phone, and a " +
                        "backup exported there imports here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
