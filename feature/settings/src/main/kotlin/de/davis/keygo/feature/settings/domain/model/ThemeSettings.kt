package de.davis.keygo.feature.settings.domain.model

import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode

/**
 * How the interface is drawn. The defaults are what the app did before this was a setting, so a
 * user who never opens the appearance section sees no change.
 */
data class ThemeSettings(
    val palette: KeyPalette = KeyPalette.System,
    val mode: ThemeMode = ThemeMode.System,
    /** Whether [KeyPalette.System] follows the wallpaper or falls back to the app's own colours. */
    val dynamicColor: Boolean = true,
)
