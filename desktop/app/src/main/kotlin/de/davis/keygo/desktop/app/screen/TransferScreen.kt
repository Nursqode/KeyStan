/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app.screen

import java.nio.file.Path
import java.nio.file.Paths
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.app.platform.FileDialogs
import de.davis.keygo.desktop.core.ui.component.KeyGoCard
import de.davis.keygo.desktop.core.vault.domain.ExportScope
import de.davis.keygo.desktop.core.vault.domain.ImportMode

/**
 * Import and export, in one screen with two modes.
 *
 * Both sides are the same act - moving a sealed backup file across a boundary - so the file picker,
 * the passphrase field and the explanation of what the format is stay identical; only the direction
 * and its consequences differ.
 */
@Composable
fun TransferScreen(viewModel: AppViewModel, state: AppState, isImport: Boolean) {
    val s = state.strings
    var pathText by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var restoreEverything by remember { mutableStateOf(false) }
    var targetVaultIndex by remember { mutableStateOf(state.selectedVaultIndex) }
    var exportEverything by remember { mutableStateOf(true) }

    val canRun = pathText.isNotBlank() && passphrase.isNotEmpty() && !state.busy

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            if (isImport) s.importTitle else s.exportTitle,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
if (isImport) s.importExplanation else s.exportExplanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pathText,
                        onValueChange = { pathText = it },
                        label = { Text(s.backupFile) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(12.dp))
                    OutlinedButton(
                        onClick = {
                            val chosen = if (isImport) {
                                FileDialogs.open(
                                    title = s.chooseBackupDialog,
                                    directory = viewModel.vaultFilePath().parent,
                                    extensions = listOf(".json", ".ksb", ".backup"),
                                )
                            } else {
                                FileDialogs.save(
                                    title = s.saveBackupDialog,
                                    suggestedName = "keystan-backup.json",
                                    directory = viewModel.vaultFilePath().parent,
                                    extensions = listOf(".json"),
                                )
                            }
                            chosen?.let { pathText = it.toString() }
                        },
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(s.choose)
                    }
                }

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(if (isImport) s.importPassphraseLabel else s.exportPassphraseLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isImport) {
                    Text(s.howApplied, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !restoreEverything, onClick = { restoreEverything = false })
                        Text(s.mergeOption)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = restoreEverything, onClick = { restoreEverything = true })
                        Text(s.replaceOption)
                    }
                    if (!restoreEverything) {
                        state.vaults.forEach { vault ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = targetVaultIndex == vault.index,
                                    onClick = { targetVaultIndex = vault.index },
                                )
                                Text(s.intoVault(vault.name))
                            }
                        }
                    } else {
                        Text(
s.restoreWarning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(s.whatContains, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = exportEverything, onClick = { exportEverything = true })
                        Text(s.everyVault)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !exportEverything, onClick = { exportEverything = false })
                        Text(s.onlySelectedVault)
                    }
                }

                Button(
                    onClick = {
                        val path: Path = Paths.get(pathText.trim())
                        if (isImport) {
                            val mode = if (restoreEverything) ImportMode.Restore
                            else ImportMode.Merge(targetVaultIndex)
                            viewModel.importFrom(path, passphrase.toCharArray(), mode)
                        } else {
                            val scope = if (exportEverything) ExportScope.Everything
                            else ExportScope.SingleVault(state.selectedVaultIndex)
                            viewModel.exportTo(path, passphrase.toCharArray(), scope)
                        }
                    },
                    enabled = canRun,
                ) {
                    Text(if (isImport) s.importAction else s.exportAction)
                }
            }
        }

        Text(
s.transferFootnote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
