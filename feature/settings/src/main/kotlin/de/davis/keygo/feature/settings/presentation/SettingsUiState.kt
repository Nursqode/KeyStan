package de.davis.keygo.feature.settings.presentation

import de.davis.keygo.core.security.domain.model.LockInfo
import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode

internal data class SettingsUiState(
    val autofillEnabled: Boolean = false,
    val chromeAutofillEnabled: Boolean = false,
    val biometricsAvailable: Boolean = false,
    val biometricsEnabled: Boolean = false,
    /** An enrollment or removal is running, so the toggle takes no further input until it ends. */
    val biometricsUpdating: Boolean = false,
    val version: String = "2.0.0",
    /** When the newest successful backup finished, or `null` while none has. */
    val lastBackupAt: Long? = null,
    val lockTimeout: LockInfo.Timeout = LockInfo.Timeout.IMMEDIATELY,
    val palette: KeyPalette = KeyPalette.System,
    val themeMode: ThemeMode = ThemeMode.System,
    /**
     * Whether the wallpaper supplies the colours. Only [KeyPalette.System] can follow it, so the
     * appearance section hides the switch under every other palette rather than lie about it.
     */
    val dynamicColor: Boolean = true,
)
