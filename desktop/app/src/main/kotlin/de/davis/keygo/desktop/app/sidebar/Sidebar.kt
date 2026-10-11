/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.davis.keygo.desktop.AppMark
import de.davis.keygo.desktop.app.AppDialog
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.NavIcons
import de.davis.keygo.desktop.app.Screen
import de.davis.keygo.desktop.app.VaultIcons
import de.davis.keygo.desktop.core.ui.component.IconBadge
import de.davis.keygo.desktop.core.ui.i18n.Strings
import de.davis.keygo.desktop.core.ui.component.SelectableRow
import de.davis.keygo.desktop.core.vault.domain.VaultSummary

/**
 * The left rail: the vaults, the other screens, and the way back out.
 *
 * Details and secrets stay in the main pane; the rail is navigation only, so that "where am I"
 * never competes with "what am I looking at".
 */
@Composable
fun Sidebar(viewModel: AppViewModel, state: AppState) {
    val s = state.strings
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxHeight().width(300.dp),
    ) {
        Column(Modifier.fillMaxHeight().padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppMark()
                Column {
                    Text("KeyStan", style = MaterialTheme.typography.titleLarge)
                    Text(
                        s.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.size(20.dp))

            Text(
                s.vaultsSection,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.size(4.dp))

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(state.vaults, key = { it.index }) { vault ->
                    VaultRow(
                        s = s,
                        vault = vault,
                        selected = state.screen == Screen.VAULT && state.selectedVaultIndex == vault.index,
                        onSelect = {
                            viewModel.selectVault(vault.index)
                            viewModel.showScreen(Screen.VAULT)
                        },
                        onRename = {
                            viewModel.selectVault(vault.index)
                            viewModel.openDialog(AppDialog.RENAME_VAULT)
                        },
                        onDelete = {
                            viewModel.selectVault(vault.index)
                            viewModel.openDialog(AppDialog.CONFIRM_DELETE_VAULT)
                        },
                    )
                }
            }

            TextButton(
                onClick = { viewModel.openDialog(AppDialog.NEW_VAULT) },
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(s.newVault)
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            NavItem(NavIcons.Generator, s.generator, state.screen == Screen.GENERATOR) {
                viewModel.showScreen(Screen.GENERATOR)
            }
            NavItem(NavIcons.Import, s.importBackup, state.screen == Screen.IMPORT) {
                viewModel.showScreen(Screen.IMPORT)
            }
            NavItem(NavIcons.Export, s.exportBackup, state.screen == Screen.EXPORT) {
                viewModel.showScreen(Screen.EXPORT)
            }
            NavItem(NavIcons.Settings, s.settings, state.screen == Screen.SETTINGS) {
                viewModel.showScreen(Screen.SETTINGS)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    state.vaultFileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(8.dp))
                Button(onClick = viewModel::lock, modifier = Modifier.fillMaxWidth()) {
                    Icon(NavIcons.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(s.lock)
                }
            }
        }
    }
}

@Composable
private fun VaultRow(
    s: Strings,
    vault: VaultSummary,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    SelectableRow(selected = selected, onClick = onSelect) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(VaultIcons.vectorFor(vault.icon))
            Column(Modifier.weight(1f)) {
                Text(
                    vault.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    s.vaultCounts(vault.loginCount, vault.cardCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = s.vaultActions,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(s.rename) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(s.delete) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
