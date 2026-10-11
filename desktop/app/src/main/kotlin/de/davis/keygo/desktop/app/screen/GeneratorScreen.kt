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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.davis.keygo.desktop.app.AppState
import de.davis.keygo.desktop.app.AppViewModel
import de.davis.keygo.desktop.app.platform.rememberClipboardCopier
import de.davis.keygo.desktop.core.ui.component.KeyGoCard
import de.davis.keygo.desktop.core.ui.i18n.Strings
import de.davis.keygo.desktop.core.ui.theme.secretTextStyle
import de.davis.keygo.desktop.core.vault.domain.PasswordGenerator
import de.davis.keygo.desktop.core.vault.domain.PasswordOptions
import de.davis.keygo.desktop.core.vault.domain.PasswordStrength
import kotlin.math.roundToInt

/** Generates a password to paste into a signup form, without leaving the app. */
@Composable
fun GeneratorScreen(viewModel: AppViewModel, state: AppState) {
    val s = state.strings
    var options by remember { mutableStateOf(PasswordOptions()) }
    var password by remember { mutableStateOf(PasswordGenerator.generate(options)) }
    val copyToClipboard = rememberClipboardCopier()

    // Every control change regenerates: showing a password that no longer matches the checkboxes
    // would be worse than a wasted keystroke.
    LaunchedEffect(options) {
        password = PasswordGenerator.generate(options)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(s.passwordGenerator, style = MaterialTheme.typography.headlineSmall)

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        password.ifEmpty { s.selectCharacterSet },
                        style = MaterialTheme.typography.headlineSmall.merge(secretTextStyle),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        if (password.isNotEmpty()) {
                            copyToClipboard(password)
                            viewModel.notify(s.passwordCopied)
                        }
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = s.copyPassword)
                    }
                    IconButton(onClick = { password = PasswordGenerator.generate(options) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = s.generateAnother)
                    }
                }

                val bits = PasswordStrength.entropyBits(password)
                LinearProgressIndicator(
                    progress = { (bits / 128.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                Text(
                    s.entropyBits(bits.roundToInt(), strengthLabel(bits, s)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        KeyGoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(s.lengthLabel(options.length), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = options.length.toFloat(),
                    onValueChange = { options = options.copy(length = it.roundToInt()) },
                    valueRange = 8f..64f,
                )

                OptionSwitch(s.lowercaseLetters, options.lowercase) {
                    options = options.copy(lowercase = it)
                }
                OptionSwitch(s.uppercaseLetters, options.uppercase) {
                    options = options.copy(uppercase = it)
                }
                OptionSwitch(s.digits, options.digits) { options = options.copy(digits = it) }
                OptionSwitch(s.symbols, options.symbols) { options = options.copy(symbols = it) }
                OptionSwitch(s.avoidAmbiguous, options.avoidAmbiguous) {
                    options = options.copy(avoidAmbiguous = it)
                }
                OptionSwitch(s.avoidProblematic, options.avoidProblematic) {
                    options = options.copy(avoidProblematic = it)
                }

                OutlinedButton(onClick = {
                    val strong = PasswordOptions(length = 24)
                    options = strong
                    password = PasswordGenerator.generate(strong)
                }) {
                    Text(s.resetStrongDefault)
                }
            }
        }

        Spacer(Modifier.size(8.dp))
        Text(
s.generatorFootnote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun strengthLabel(bits: Double, s: Strings): String = when {
    bits < 40 -> s.strengthWeak
    bits < 60 -> s.strengthFair
    bits < 80 -> s.strengthStrong
    else -> s.strengthExcellent
}
