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
import de.davis.keygo.desktop.core.vault.data.AppLanguage
import de.davis.keygo.desktop.core.vault.data.ThemeMode

/** Preferences, the vault file's location, and what the file actually is. */
@Composable
fun SettingsScreen(viewModel: AppViewModel, state: AppState) {
    val s = state.strings
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(s.settings, style = MaterialTheme.typography.headlineSmall)

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.appearance, style = MaterialTheme.typography.titleMedium)
                ThemeMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.settings.theme == mode,
                            onClick = { viewModel.updateSettings { it.copy(theme = mode) } },
                        )
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> s.followSystem
                                ThemeMode.LIGHT -> s.light
                                ThemeMode.DARK -> s.dark
                            },
                        )
                    }
                }
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.language, style = MaterialTheme.typography.titleMedium)
                AppLanguage.entries.forEach { language ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.settings.language == language,
                            onClick = { viewModel.updateSettings { it.copy(language = language) } },
                        )
                        Text(
                            when (language) {
                                AppLanguage.SYSTEM -> s.languageSystem
                                AppLanguage.ENGLISH -> s.languageEnglish
                                AppLanguage.RUSSIAN -> s.languageRussian
                            },
                        )
                    }
                }
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(s.security, style = MaterialTheme.typography.titleMedium)

                Text(
                    s.lockAfterMinutes(state.settings.autoLockMinutes),
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
                    if (state.settings.autoLockMinutes == 0) s.autoLockOff
                    else s.autoLockHint,
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
                    Text(s.concealSecrets)
                }

                HorizontalDivider()
                Button(onClick = { viewModel.openDialog(AppDialog.CHANGE_PASSPHRASE) }) {
                    Text(s.changePassphraseTitle)
                }
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(s.vaultFile, style = MaterialTheme.typography.titleMedium)
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
                        Text(s.openAnotherFile)
                    }
                }
                Text(
                    s.switchingFilesHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.about, style = MaterialTheme.typography.titleMedium)
                Text(
                    s.aboutVersion(DESKTOP_VERSION),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(s.aboutAuthor, style = MaterialTheme.typography.bodyMedium)
                Text(
s.aboutCredits,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
s.aboutFormat,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
