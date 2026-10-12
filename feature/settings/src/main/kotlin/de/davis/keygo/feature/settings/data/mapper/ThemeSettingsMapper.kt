package de.davis.keygo.feature.settings.data.mapper

import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode
import de.davis.keygo.feature.settings.data.local.model.ProtoKeyPalette
import de.davis.keygo.feature.settings.data.local.model.ProtoThemeMode
import de.davis.keygo.feature.settings.data.local.model.ProtoThemeSettings
import de.davis.keygo.feature.settings.domain.model.ThemeSettings

internal fun ProtoThemeSettings.toDomain() = ThemeSettings(
    palette = palette.toDomain(),
    mode = mode.toDomain(),
    // The field is optional so that its absence keeps meaning "wallpaper colours", which is what
    // the app did before the setting existed. Reading it plainly would report proto3's false on a
    // store written by an older build and switch Material You off without the user asking.
    dynamicColor = if (hasDynamicColor()) dynamicColor else true,
)

internal fun KeyPalette.toProto() = when (this) {
    KeyPalette.System -> ProtoKeyPalette.PALETTE_SYSTEM
    KeyPalette.Red -> ProtoKeyPalette.PALETTE_RED
}

internal fun ThemeMode.toProto() = when (this) {
    ThemeMode.System -> ProtoThemeMode.THEME_SYSTEM
    ThemeMode.Light -> ProtoThemeMode.THEME_LIGHT
    ThemeMode.Dark -> ProtoThemeMode.THEME_DARK
}

// System is the proto3 default and also the fallback for anything this build does not know: a user
// who never picked a palette must not be handed one, and protobuf keeps enum numbers written by a
// newer version (they surface as UNRECOGNIZED), so the mapping has to end in an else.
private fun ProtoKeyPalette.toDomain() = when (this) {
    ProtoKeyPalette.PALETTE_RED -> KeyPalette.Red
    else -> KeyPalette.System
}

private fun ProtoThemeMode.toDomain() = when (this) {
    ProtoThemeMode.THEME_LIGHT -> ThemeMode.Light
    ProtoThemeMode.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}
