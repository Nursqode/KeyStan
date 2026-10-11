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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.davis.keygo.desktop.AppMark
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.app.platform.FileDialogs
import de.davis.keygo.desktop.core.ui.component.KeyGoCard
import de.davis.keygo.desktop.core.vault.domain.PassphrasePolicy

/**
 * The gate in front of everything else.
 *
 * It is one screen with two modes rather than two screens: whether a vault exists at the configured
 * path is not something the user chose, and asking them to pick "create" or "open" would make them
 * decide something the app already knows.
 */
@Composable
fun LockScreen(viewModel: AppViewModel, state: AppState) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val creating = !state.hasVaultFile

    val longEnough = passphrase.length >= PassphrasePolicy.MIN_LENGTH
    val matches = !creating || passphrase == confirmation
    val canSubmit = passphrase.isNotEmpty() && matches && (!creating || longEnough) && !state.busy

    fun submit() {
        if (!canSubmit) return
        viewModel.submitPassphrase(passphrase.toCharArray())
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        KeyGoCard(modifier = Modifier.width(460.dp)) {
            Column(
                modifier = Modifier.padding(36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppMark(size = 64)
                Text(
                    text = if (creating) "Create your vault" else "Unlock KeyStan",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (creating) {
                        "Your vault is sealed with this passphrase. It is never stored, and there is " +
                            "no recovery - write it down."
                    } else {
                        "Enter the master passphrase for ${state.vaultFileName}."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Master passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (creating) ImeAction.Next else ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (creating) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text("Repeat passphrase") },
                        singleLine = true,
                        isError = confirmation.isNotEmpty() && confirmation != passphrase,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        supportingText = if (confirmation.isNotEmpty() && confirmation != passphrase)
                            ({ Text("The two passphrases do not match") }) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (passphrase.isNotEmpty() && !longEnough) {
                        Text(
                            "Use at least ${PassphrasePolicy.MIN_LENGTH} characters",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                state.lockError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = { submit() },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (creating) "Create vault" else "Unlock")
                }

                TextButton(
                    onClick = {
                        FileDialogs.open("Open a vault file", viewModel.vaultFilePath().parent)
                            ?.let(viewModel::useVaultFile)
                    },
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Text("  Open a different vault file")
                }

                Text(
                    text = viewModel.vaultFilePath().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
