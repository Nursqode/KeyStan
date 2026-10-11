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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.davis.keygo.desktop.app.AppDialog
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.app.ItemFormatting
import de.davis.keygo.desktop.app.ItemRow
import de.davis.keygo.desktop.app.VaultIcons
import de.davis.keygo.desktop.app.platform.rememberClipboardCopier
import de.davis.keygo.desktop.core.ui.component.DetailRow
import de.davis.keygo.desktop.core.ui.component.EmptyState
import de.davis.keygo.desktop.core.ui.component.IconBadge
import de.davis.keygo.desktop.core.ui.component.KeyGoCard
import de.davis.keygo.desktop.core.ui.component.Pill
import de.davis.keygo.desktop.core.ui.component.SearchField
import de.davis.keygo.desktop.core.ui.component.SelectableRow
import de.davis.keygo.desktop.core.ui.theme.secretTextStyle
import de.davis.keygo.desktop.core.vault.domain.Item
import de.davis.keygo.desktop.core.vault.domain.ItemKind
import de.davis.keygo.desktop.core.vault.domain.ItemRef
import de.davis.keygo.desktop.core.vault.domain.Totp

/**
 * The main screen: what is in the vault on the left, the selected item on the right.
 *
 * A desktop window is wide enough to show both at once, which removes the phone's back-and-forth
 * without changing what an item is.
 */
@Composable
fun VaultScreen(viewModel: AppViewModel, state: AppState) {
    Row(Modifier.fillMaxSize()) {
        ItemList(viewModel, state, Modifier.width(420.dp).fillMaxHeight())
        VerticalDivider()
        DetailPane(viewModel, state, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun ItemList(viewModel: AppViewModel, state: AppState, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.selectedVault?.name ?: "Vault",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${state.rows.size} items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { viewModel.startNewItem(ItemKind.LOGIN) }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Login")
            }
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = { viewModel.startNewItem(ItemKind.CARD) }) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Card")
            }
        }

        SearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.padding(horizontal = 20.dp),
            placeholder = "Search this vault",
        )
        Spacer(Modifier.size(12.dp))

        if (state.rows.isEmpty()) {
            EmptyState(
                icon = if (state.query.isEmpty()) Icons.Filled.Key else Icons.Filled.Pin,
                title = if (state.query.isEmpty()) "Nothing here yet" else "No matches",
                subtitle = if (state.query.isEmpty()) {
                    "Add a login or a card, or import a backup from your phone."
                } else {
                    "Nothing in this vault matches \"${state.query}\"."
                },
            )
        } else {
            LazyColumn {
                items(state.rows, key = { "${it.ref.vaultIndex}-${it.ref.kind}-${it.ref.itemIndex}" }) { row ->
                    ItemListRow(
                        row = row,
                        selected = state.selectedItem == row.ref,
                        nowSeconds = state.totpNowSeconds,
                        onClick = { viewModel.selectItem(row.ref) },
                        onTogglePin = { viewModel.togglePinned(row.ref) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemListRow(
    row: ItemRow,
    selected: Boolean,
    nowSeconds: Long,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
) {
    SelectableRow(selected = selected, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(
                icon = if (row.kind == ItemKind.LOGIN) Icons.Filled.Key else Icons.Filled.CreditCard,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    row.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    row.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            row.totpSecret?.let { secret ->
                TotpCode(secret = secret, nowSeconds = nowSeconds, compact = true)
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (row.pinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (row.pinned) "Unpin" else "Pin",
                    tint = if (row.pinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The live one-time code, with the seconds left before it changes. */
@Composable
private fun TotpCode(secret: String, nowSeconds: Long, compact: Boolean) {
    val parameters = remember(secret) { Totp.parametersFor(secret) }
    if (parameters == null) {
        Pill("no code")
        return
    }
    val code = remember(parameters, nowSeconds) {
        Totp.codeOrNull(
            base32Secret = parameters.secret,
            timeSeconds = nowSeconds,
            digits = parameters.digits,
            period = parameters.period,
            algorithm = parameters.algorithm,
        )
    }
    val remaining = Totp.remainingSeconds(nowSeconds, parameters.period)

    if (code == null) {
        Pill("no code")
        return
    }

    if (compact) {
        Column(horizontalAlignment = Alignment.End) {
            Text(code, style = secretTextStyle, color = MaterialTheme.colorScheme.primary)
            Text(
                "${remaining}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column {
            Text(code, style = secretTextStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(6.dp))
            LinearProgressIndicator(
                progress = { remaining.toFloat() / parameters.period },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
            Text(
                "Changes in ${remaining}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailPane(viewModel: AppViewModel, state: AppState, modifier: Modifier = Modifier) {
    val copyToClipboard = rememberClipboardCopier()

    fun copy(label: String, value: String?) {
        if (value.isNullOrEmpty()) return
        copyToClipboard(value)
        viewModel.notify("$label copied")
    }

    Box(modifier) {
        val item = state.detail
        if (item == null) {
            EmptyState(
                icon = Icons.Filled.Key,
                title = "Nothing selected",
                subtitle = "Pick an item on the left, or add a new one.",
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = when (item) {
                        is Item.OfLogin -> Icons.Filled.Key
                        is Item.OfCard -> Icons.Filled.CreditCard
                    },
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (item.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.tags.forEach { Pill(it) }
                        }
                    }
                }
                ItemActions(viewModel, state, item)
            }

            when (item) {
                is Item.OfLogin -> LoginDetails(state, item, ::copy)
                is Item.OfCard -> CardDetails(state, item, ::copy)
            }
            item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                KeyGoCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Notes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemActions(viewModel: AppViewModel, state: AppState, item: Item) {
    var moveMenu by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        // One reveal switch for the whole screen: a password and a CVV shown together is what the
        // user is doing anyway, and it makes the concealed state the default on every item.
        IconButton(onClick = viewModel::toggleRevealSecrets) {
            Icon(
                imageVector = if (state.revealSecrets) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (state.revealSecrets) "Hide secrets" else "Show secrets",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = { state.selectedItem?.let(viewModel::startEditItem) }) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Edit")
        }
        Box {
            OutlinedButton(onClick = { moveMenu = true }) { Text("Move") }
            DropdownMenu(expanded = moveMenu, onDismissRequest = { moveMenu = false }) {
                state.vaults.filter { it.index != state.selectedVaultIndex }.forEach { vault ->
                    DropdownMenuItem(
                        text = { Text("Move to ${vault.name}") },
                        onClick = {
                            moveMenu = false
                            state.selectedItem?.let { viewModel.moveItem(it, vault.index) }
                        },
                    )
                }
            }
        }
        IconButton(onClick = { viewModel.openDialog(AppDialog.CONFIRM_DELETE_ITEM) }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun LoginDetails(state: AppState, item: Item.OfLogin, onCopy: (String, String?) -> Unit) {
    val login = item.login
    val reveal = state.revealSecrets

    KeyGoCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            DetailRow(
                label = "Username",
                value = login.username,
                onCopy = { onCopy("Username", login.username) },
            )
            HorizontalDivider()
            DetailRow(
                label = "Password",
                value = login.password,
                isSecret = true,
                revealed = reveal,
                onCopy = { onCopy("Password", login.password) },
            )
            login.totpSecret?.let { secret ->
                HorizontalDivider()
                Spacer(Modifier.size(12.dp))
                Text(
                    "One-time code",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(6.dp))
                TotpCode(secret = secret, nowSeconds = state.totpNowSeconds, compact = false)
            }
            if (login.websites.isNotEmpty()) {
                HorizontalDivider()
                login.websites.forEach { website ->
                    DetailRow(label = "Website", value = website, onCopy = { onCopy("Website", website) })
                }
            }
        }
    }
}

@Composable
private fun CardDetails(state: AppState, item: Item.OfCard, onCopy: (String, String?) -> Unit) {
    val card = item.card
    val reveal = state.revealSecrets

    KeyGoCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            DetailRow(
                label = "Cardholder",
                value = card.cardholder,
                onCopy = { onCopy("Cardholder", card.cardholder) },
            )
            HorizontalDivider()
            DetailRow(
                label = "Number",
                value = ItemFormatting.formatCardNumber(card.number),
                isSecret = false,
                revealed = reveal,
                onCopy = { onCopy("Card number", card.number.replace(" ", "")) },
            )
            HorizontalDivider()
            DetailRow(label = "Expires", value = ItemFormatting.expiry(card))
            HorizontalDivider()
            DetailRow(
                label = "CVV",
                value = card.cvv,
                isSecret = true,
                revealed = reveal,
                onCopy = { onCopy("CVV", card.cvv) },
            )
        }
    }
}

/** The vault's icon, exposed for the sidebar and the vault dialogs. */
@Composable
internal fun VaultIconVector(icon: String) = VaultIcons.vectorFor(icon)
