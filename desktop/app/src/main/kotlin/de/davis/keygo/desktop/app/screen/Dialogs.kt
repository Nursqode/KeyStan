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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.davis.keygo.desktop.app.AppDialog
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.app.ItemDraft
import de.davis.keygo.desktop.app.VaultIcons
import de.davis.keygo.desktop.core.backup.model.Card
import de.davis.keygo.desktop.core.backup.model.Login
import de.davis.keygo.desktop.core.ui.component.IconBadge
import de.davis.keygo.desktop.core.vault.domain.ItemKind
import de.davis.keygo.desktop.core.vault.domain.PasswordGenerator
import de.davis.keygo.desktop.core.vault.domain.PasswordOptions
import de.davis.keygo.desktop.core.vault.domain.PassphrasePolicy

/**
 * Every modal in the app, dispatched from one place.
 *
 * The dialogs are small enough that keeping them together makes the state transitions - which
 * dialog a click opens, and what saving it does - readable in one pass.
 */
@Composable
fun Dialogs(viewModel: AppViewModel, state: AppState) {
    when (state.dialog) {
        AppDialog.NONE -> Unit

        AppDialog.NEW_VAULT -> VaultDialog(
            title = "New vault",
            initialName = "",
            initialIcon = VaultIcons.Default,
            onDismiss = viewModel::closeDialog,
            onConfirm = { name, icon ->
                viewModel.createVault(name, icon)
                viewModel.closeDialog()
            },
        )

        AppDialog.RENAME_VAULT -> {
            val vault = state.selectedVault
            VaultDialog(
                title = "Rename vault",
                initialName = vault?.name.orEmpty(),
                initialIcon = vault?.icon ?: VaultIcons.Default,
                onDismiss = viewModel::closeDialog,
                onConfirm = { name, icon ->
                    viewModel.renameVault(state.selectedVaultIndex, name, icon)
                    viewModel.closeDialog()
                },
            )
        }

        AppDialog.EDIT_ITEM -> state.editing?.let { draft ->
            ItemEditorDialog(
                draft = draft,
                onDismiss = viewModel::closeDialog,
                onSaveLogin = { login ->
                    viewModel.saveLogin(draft, login)
                    viewModel.closeDialog()
                },
                onSaveCard = { card ->
                    viewModel.saveCard(draft, card)
                    viewModel.closeDialog()
                },
            )
        }

        AppDialog.CONFIRM_DELETE_ITEM -> ConfirmDialog(
            title = "Delete this item?",
            text = "The item is removed from the vault and the file is rewritten. This cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = viewModel::closeDialog,
            onConfirm = {
                state.selectedItem?.let(viewModel::deleteItem)
                viewModel.closeDialog()
            },
        )

        AppDialog.CONFIRM_DELETE_VAULT -> ConfirmDialog(
            title = "Delete \"${state.selectedVault?.name.orEmpty()}\"?",
            text = "Every login and card in this vault is deleted with it.",
            confirmLabel = "Delete vault",
            onDismiss = viewModel::closeDialog,
            onConfirm = {
                viewModel.deleteVault(state.selectedVaultIndex)
                viewModel.closeDialog()
            },
        )

        AppDialog.CHANGE_PASSPHRASE -> ChangePassphraseDialog(
            onDismiss = viewModel::closeDialog,
            onConfirm = { newPassphrase ->
                viewModel.changePassphrase(newPassphrase)
                viewModel.closeDialog()
            },
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun VaultDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.heightIn(max = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(VaultIcons.names) { candidate ->
                        IconButton(onClick = { icon = candidate }) {
                            IconBadge(
                                icon = VaultIcons.vectorFor(candidate),
                                containerColor = if (candidate == icon) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim().ifBlank { "Vault" }, icon) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChangePassphraseDialog(onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = passphrase.length >= PassphrasePolicy.MIN_LENGTH && passphrase == confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change the master passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The vault file is re-sealed with the new passphrase. There is no recovery if " +
                        "it is lost.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("New passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Repeat passphrase") },
                    singleLine = true,
                    isError = confirmation.isNotEmpty() && confirmation != passphrase,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (passphrase.isNotEmpty() && passphrase.length < PassphrasePolicy.MIN_LENGTH) {
                    Text(
                        "Use at least ${PassphrasePolicy.MIN_LENGTH} characters",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(passphrase.toCharArray()) }, enabled = valid) { Text("Change") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * The item editor.
 *
 * A login and a card are edited in the same dialog because they are the same kind of thing to the
 * user - an entry with a title, notes and tags plus a few type-specific fields - and a new item can
 * switch between them without being recreated.
 */
@Composable
private fun ItemEditorDialog(
    draft: ItemDraft,
    onDismiss: () -> Unit,
    onSaveLogin: (Login) -> Unit,
    onSaveCard: (Card) -> Unit,
) {
    var kind by remember { mutableStateOf(draft.kind) }
    var title by remember { mutableStateOf(if (kind == ItemKind.LOGIN) draft.login.title else draft.card.title) }
    var notes by remember { mutableStateOf(if (kind == ItemKind.LOGIN) draft.login.notes.orEmpty() else draft.card.notes.orEmpty()) }
    var tags by remember { mutableStateOf((if (kind == ItemKind.LOGIN) draft.login.tags else draft.card.tags).joinToString(", ")) }
    var pinned by remember { mutableStateOf(if (kind == ItemKind.LOGIN) draft.login.pinned else draft.card.pinned) }

    var username by remember { mutableStateOf(draft.login.username.orEmpty()) }
    var password by remember { mutableStateOf(draft.login.password.orEmpty()) }
    var websites by remember { mutableStateOf(draft.login.websites.joinToString("\n")) }
    var totpSecret by remember { mutableStateOf(draft.login.totpSecret.orEmpty()) }

    var cardholder by remember { mutableStateOf(draft.card.cardholder.orEmpty()) }
    var number by remember { mutableStateOf(draft.card.number) }
    var expiryMonth by remember { mutableStateOf(draft.card.expirationMonth?.toString().orEmpty()) }
    var expiryYear by remember { mutableStateOf(draft.card.expirationYear?.toString().orEmpty()) }
    var cvv by remember { mutableStateOf(draft.card.cvv.orEmpty()) }

    val tagList = remember(tags) { tags.split(',').map { it.trim() }.filter { it.isNotEmpty() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.ref == null) "New item" else "Edit item") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (draft.ref == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (kind == ItemKind.LOGIN) {
                            Button(onClick = { kind = ItemKind.LOGIN }) { Text("Login") }
                        } else {
                            OutlinedButton(onClick = { kind = ItemKind.LOGIN }) { Text("Login") }
                        }
                        if (kind == ItemKind.CARD) {
                            Button(onClick = { kind = ItemKind.CARD }) { Text("Card") }
                        } else {
                            OutlinedButton(onClick = { kind = ItemKind.CARD }) { Text("Card") }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (kind == ItemKind.LOGIN) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { password = PasswordGenerator.generate(PasswordOptions(length = 20)) },
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Generate a password")
                        }
                    }
                    OutlinedTextField(
                        value = websites,
                        onValueChange = { websites = it },
                        label = { Text("Websites (one per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = totpSecret,
                        onValueChange = { totpSecret = it },
                        label = { Text("TOTP secret or otpauth:// URI") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = cardholder,
                        onValueChange = { cardholder = it },
                        label = { Text("Cardholder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Card number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = expiryMonth,
                            onValueChange = { expiryMonth = it.filter(Char::isDigit).take(2) },
                            label = { Text("Month") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = expiryYear,
                            onValueChange = { expiryYear = it.filter(Char::isDigit).take(4) },
                            label = { Text("Year") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it.filter(Char::isDigit).take(4) },
                            label = { Text("CVV") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                    Spacer(Modifier.width(12.dp))
                    Text("Pinned")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (kind == ItemKind.LOGIN) {
                        onSaveLogin(
                            Login(
                                title = title,
                                notes = notes.ifBlank { null },
                                tags = tagList,
                                pinned = pinned,
                                username = username.ifBlank { null },
                                password = password.ifBlank { null },
                                totpSecret = totpSecret.ifBlank { null },
                                websites = websites.lines().map { it.trim() }.filter { it.isNotEmpty() },
                                passkeys = draft.login.passkeys,
                            ),
                        )
                    } else {
                        onSaveCard(
                            Card(
                                title = title,
                                notes = notes.ifBlank { null },
                                tags = tagList,
                                pinned = pinned,
                                cardholder = cardholder.ifBlank { null },
                                number = number.replace(" ", ""),
                                expirationMonth = expiryMonth.toIntOrNull(),
                                expirationYear = expiryYear.toIntOrNull()?.let { if (it < 100) 2000 + it else it },
                                cvv = cvv.ifBlank { null },
                            ),
                        )
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
