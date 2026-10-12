package de.davis.keygo.core.ui.theme

/**
 * Whether the interface is drawn light or dark.
 *
 * [System] is the default and was the only behaviour before there was a setting, so a device that
 * turns dark at sunset keeps doing that on its own.
 */
enum class ThemeMode {
    System,
    Light,
    Dark,
}

/**
 * Resolves this mode against the device. The caller reads [systemInDarkTheme] and passes it in:
 * only a composable can ask, and keeping that out of here leaves the mapping testable.
 */
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemInDarkTheme
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
