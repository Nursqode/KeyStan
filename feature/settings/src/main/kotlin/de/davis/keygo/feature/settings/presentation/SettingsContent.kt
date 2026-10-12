package de.davis.keygo.feature.settings.presentation

import android.text.format.DateUtils
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.davis.keygo.core.security.domain.model.LockInfo
import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode
import de.davis.keygo.core.ui.theme.isDark
import de.davis.keygo.core.ui.theme.themeColorScheme
import de.davis.keygo.core.util.presentation.UIText
import de.davis.keygo.core.util.presentation.UIText.Companion.PluralsString
import de.davis.keygo.core.util.presentation.UIText.Companion.ResourceString
import de.davis.keygo.feature.settings.R
import de.davis.keygo.feature.settings.presentation.component.SectionScope
import de.davis.keygo.feature.settings.presentation.component.SettingsList

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
) {
    val defaultColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        leadingContentColor = MaterialTheme.colorScheme.primaryContainer,
    )
    val warningColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        supportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        leadingContentColor = MaterialTheme.colorScheme.primary,
        trailingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        // Resolved once: every swatch has to agree with the interface, so the whole section reads a
        // single light/dark answer instead of each sample guessing its own.
        val isDarkMode = state.themeMode.isDark(isSystemInDarkTheme())
        val samples = paletteSamples(isDark = isDarkMode, dynamicColor = state.dynamicColor)

        SettingsList(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = innerPadding,
        ) {
            section(title = R.string.settings_appearance) {
                picker(
                    title = R.string.settings_theme_palette,
                    icon = Icons.Default.Palette,
                    colors = defaultColors,
                    selected = state.palette,
                    options = KeyPalette.entries,
                    label = { it.label },
                    sample = { palette -> samples.getValue(palette) },
                    onSelect = { onEvent(SettingsUiEvent.SetThemePalette(it)) },
                )

                picker(
                    title = R.string.settings_theme,
                    icon = Icons.Default.DarkMode,
                    colors = defaultColors,
                    selected = state.themeMode,
                    options = ThemeMode.entries,
                    label = { it.label },
                    onSelect = { onEvent(SettingsUiEvent.SetThemeMode(it)) },
                )

                // Only the system palette can follow the wallpaper; under any other palette the
                // switch would change nothing, so it stays out of the way instead of lying.
                if (state.palette == KeyPalette.System) toggle(
                    title = R.string.settings_theme_dynamic_color,
                    icon = Icons.Default.Wallpaper,
                    supporting = ResourceString(R.string.settings_theme_dynamic_color_description),
                    colors = defaultColors,
                    checked = state.dynamicColor,
                    onCheckedChange = { onEvent(SettingsUiEvent.SetDynamicColor(it)) },
                )
            }

            section(title = R.string.settings_security) {
                action(
                    title = R.string.settings_reset_password,
                    icon = Icons.Default.LockReset,
                    supporting = ResourceString(R.string.settings_reset_password_description),
                    colors = defaultColors,
                    onClick = { onEvent(SettingsUiEvent.ResetPassword) },
                )

                if (state.biometricsAvailable) toggle(
                    title = R.string.settings_use_biometrics,
                    icon = Icons.Default.Fingerprint,
                    supporting = ResourceString(R.string.settings_use_biometrics_description),
                    colors = defaultColors,
                    checked = state.biometricsEnabled,
                    enabled = !state.biometricsUpdating,
                    onCheckedChange = { onEvent(SettingsUiEvent.SetBiometrics(it)) },
                )

                toggle(
                    title = R.string.settings_autofill,
                    icon = Icons.Default.Password,
                    supporting = ResourceString(R.string.settings_autofill_description),
                    colors = defaultColors,
                    checked = state.autofillEnabled,
                    onCheckedChange = { onEvent(SettingsUiEvent.SetAutofill(it)) },
                )

                autofillEntries(state, warningColors, onEvent)

                picker(
                    title = R.string.settings_auto_lock,
                    icon = Icons.Default.Lock,
                    colors = defaultColors,
                    selected = state.lockTimeout,
                    options = LockInfo.Timeout.entries,
                    label = { it.label },
                    onSelect = { onEvent(SettingsUiEvent.SetAutoLockTimeout(it)) },
                )
            }

            section(title = R.string.settings_backup) {
                action(
                    title = R.string.settings_backup_and_restore,
                    icon = Icons.Default.Backup,
                    supporting = lastBackupText(state.lastBackupAt),
                    colors = defaultColors,
                    onClick = { onEvent(SettingsUiEvent.OpenBackup) },
                )
            }

            section(title = R.string.settings_about) {
                action(
                    title = R.string.settings_3rd_party_licenses,
                    icon = Icons.Default.Code,
                    colors = defaultColors,
                    onClick = { onEvent(SettingsUiEvent.LibrariesClicked) },
                )

                action(
                    title = R.string.settings_report_issue,
                    icon = Icons.Default.BugReport,
                    navigationIcon = Icons.AutoMirrored.Default.OpenInNew,
                    colors = defaultColors,
                    onClick = { onEvent(SettingsUiEvent.ReportIssue) }
                )

                value(
                    title = R.string.settings_version,
                    value = state.version,
                    icon = Icons.Default.Update,
                    colors = defaultColors,
                )
            }
        }
    }
}

internal fun SectionScope.autofillEntries(
    state: SettingsUiState,
    warningColors: ListItemColors,
    onEvent: (SettingsUiEvent) -> Unit,
) {
    if (state.autofillEnabled && !state.chromeAutofillEnabled) {
        action(
            title = R.string.settings_autofill_finish_setup,
            icon = Icons.Default.Public,
            supporting = ResourceString(R.string.settings_autofill_finish_setup_needs_chrome),
            colors = warningColors,
            navigationIcon = Icons.AutoMirrored.Default.OpenInNew,
            onClick = { onEvent(SettingsUiEvent.OpenChromeAutofillSettings) },
        )
    }
}

/**
 * Relative rather than absolute ("2 days ago", not a date): what matters at a glance is how stale
 * the newest backup is, so the row doubles as a nudge once it starts reading in weeks.
 */
private fun lastBackupText(lastBackupAt: Long?): UIText = when (lastBackupAt) {
    null -> ResourceString(R.string.settings_backup_none)
    else -> ResourceString(
        R.string.settings_backup_last,
        DateUtils.getRelativeTimeSpanString(
            lastBackupAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString(),
    )
}

private val LockInfo.Timeout.label: UIText
    get() = when (this) {
        LockInfo.Timeout.IMMEDIATELY -> ResourceString(R.string.settings_auto_lock_immediately)
        else -> PluralsString(
            R.plurals.settings_auto_lock_n_minute,
            duration.inWholeMinutes.toInt(),
            duration.inWholeMinutes,
        )
    }

private val KeyPalette.label: UIText
    get() = when (this) {
        KeyPalette.System -> ResourceString(R.string.settings_palette_system)
        KeyPalette.Red -> ResourceString(R.string.settings_palette_red)
    }

private val ThemeMode.label: UIText
    get() = when (this) {
        ThemeMode.System -> ResourceString(R.string.settings_theme_system)
        ThemeMode.Light -> ResourceString(R.string.settings_theme_light)
        ThemeMode.Dark -> ResourceString(R.string.settings_theme_dark)
    }

/**
 * The swatch for every palette.
 *
 * A palette with a seed shows the seed itself, so a chip that says red is red in light and in dark.
 * Material deliberately draws an accent in a light tone on a dark background, and a chip showing
 * that derived tone would read as pink next to its own name. [KeyPalette.System] has no colour of
 * its own, so it shows what it resolves to — the wallpaper or the inherited violet — which is what
 * that choice actually delivers.
 */
@Composable
private fun paletteSamples(isDark: Boolean, dynamicColor: Boolean): Map<KeyPalette, Color> =
    KeyPalette.entries.associateWith { palette ->
        palette.seed
            ?: themeColorScheme(isDarkMode = isDark, dynamicColor = dynamicColor, palette = palette).primary
    }

@Preview
@Composable
private fun SettingsContentPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            SettingsContent(
                state = SettingsUiState(
                    autofillEnabled = true,
                    lockTimeout = LockInfo.Timeout.TWO_MINUTES,
                ),
                onEvent = {},
            )
        }
    }
}
