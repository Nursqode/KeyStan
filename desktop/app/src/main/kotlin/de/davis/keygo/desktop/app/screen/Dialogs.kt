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
import de.davis.keygo.desktop.core.ui.i18n.Strings
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
    val s = state.strings
    when (state.dialog) {
        AppDialog.NONE -> Unit

        AppDialog.NEW_VAULT -> VaultDialog(
            s = s,
            title = s.newVault,
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
                s = s,
                title = s.renameVault,
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
                s = s,
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
            title = s.deleteItemTitle,
            text = s.deleteItemText,
            confirmLabel = s.delete,
            cancelLabel = s.cancel,
            onDismiss = viewModel::closeDialog,
            onConfirm = {
                state.selectedItem?.let(viewModel::deleteItem)
                viewModel.closeDialog()
            },
        )

        AppDialog.CONFIRM_DELETE_VAULT -> ConfirmDialog(
            title = s.deleteVaultTitle(state.selectedVault?.name.orEmpty()),
            text = s.deleteVaultText,
            confirmLabel = s.deleteVault,
            cancelLabel = s.cancel,
            onDismiss = viewModel::closeDialog,
            onConfirm = {
                viewModel.deleteVault(state.selectedVaultIndex)
                viewModel.closeDialog()
            },
        )

        AppDialog.CHANGE_PASSPHRASE -> ChangePassphraseDialog(
            s = s,
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
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
    )
}

@Composable
private fun VaultDialog(
    s: Strings,
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
                    label = { Text(s.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(s.icon, style = MaterialTheme.typography.labelMedium)
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
                onClick = { onConfirm(name.trim().ifBlank { s.vaultFallback }, icon) },
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
private fun ChangePassphraseDialog(s: Strings, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = passphrase.length >= PassphrasePolicy.MIN_LENGTH && passphrase == confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.changePassphraseTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(s.changePassphraseText, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(s.newPassphrase) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text(s.repeatPassphrase) },
                    singleLine = true,
                    isError = confirmation.isNotEmpty() && confirmation != passphrase,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (passphrase.isNotEmpty() && passphrase.length < PassphrasePolicy.MIN_LENGTH) {
                    Text(
                        s.atLeastCharacters(PassphrasePolicy.MIN_LENGTH),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(passphrase.toCharArray()) }, enabled = valid) { Text(s.change) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
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
    s: Strings,
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
        title = { Text(if (draft.ref == null) s.newItem else s.editItem) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (draft.ref == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (kind == ItemKind.LOGIN) {
                            Button(onClick = { kind = ItemKind.LOGIN }) { Text(s.login) }
                        } else {
                            OutlinedButton(onClick = { kind = ItemKind.LOGIN }) { Text(s.login) }
                        }
                        if (kind == ItemKind.CARD) {
                            Button(onClick = { kind = ItemKind.CARD }) { Text(s.card) }
                        } else {
                            OutlinedButton(onClick = { kind = ItemKind.CARD }) { Text(s.card) }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s.titleField) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (kind == ItemKind.LOGIN) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(s.username) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(s.password) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { password = PasswordGenerator.generate(PasswordOptions(length = 20)) },
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = s.generatePassword)
                        }
                    }
                    OutlinedTextField(
                        value = websites,
                        onValueChange = { websites = it },
                        label = { Text(s.websitesLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = totpSecret,
                        onValueChange = { totpSecret = it },
                        label = { Text(s.totpSecretLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = cardholder,
                        onValueChange = { cardholder = it },
                        label = { Text(s.cardholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text(s.cardNumber) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = expiryMonth,
                            onValueChange = { expiryMonth = it.filter(Char::isDigit).take(2) },
                            label = { Text(s.month) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = expiryYear,
                            onValueChange = { expiryYear = it.filter(Char::isDigit).take(4) },
                            label = { Text(s.year) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it.filter(Char::isDigit).take(4) },
                            label = { Text(s.cvv) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(s.tagsLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(s.notes) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                    Spacer(Modifier.width(12.dp))
                    Text(s.pinned)
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
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
