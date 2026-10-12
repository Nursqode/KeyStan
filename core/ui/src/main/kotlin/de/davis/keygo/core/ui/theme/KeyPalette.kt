package de.davis.keygo.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent

/**
 * The palette the interface is drawn in.
 *
 * [System] leaves the choice to the device: with wallpaper colours on that is Material You, and with
 * them off it is the violet scheme this fork inherited. [Red] is generated from its [seed] by
 * Material's own colour utilities, which is what makes a palette called [Red] actually look red.
 * Deriving it by rotating the inherited scheme's hue — what this used to do — kept that scheme's
 * muted saturation and dragged the other accent families into unrelated colours, so "red" came out
 * brown with a green tertiary.
 *
 * The generator is Material's "content" scheme rather than its tonal default: content keeps the
 * seed's own colour, so the primary is the red that was asked for and the containers carry it too,
 * where the tonal default would mute it into brick.
 */
enum class KeyPalette(val seed: Color?) {
    System(null),

    Red(Color(0xFFB3261E)),
}

/**
 * The scheme this palette paints [isDark] in at [contrastLevel], where 0 is Material's own contrast
 * and 1 the highest the spec offers.
 *
 * `null` means the app's own scheme: [KeyPalette.System] is whatever the device or the inherited
 * look says, and it must not be redrawn by machinery that exists only to draw the seed palettes.
 */
internal fun KeyPalette.scheme(isDark: Boolean, contrastLevel: Double): ColorScheme? {
    val seed = seed ?: return null
    val dynamic = SchemeContent(Hct.fromInt(seed.toArgb()), isDark, contrastLevel)
    val colors = MaterialDynamicColors()

    return (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = dynamic.paint(colors.primary()),
        onPrimary = dynamic.paint(colors.onPrimary()),
        primaryContainer = dynamic.paint(colors.primaryContainer()),
        onPrimaryContainer = dynamic.paint(colors.onPrimaryContainer()),
        inversePrimary = dynamic.paint(colors.inversePrimary()),
        primaryFixed = dynamic.paint(colors.primaryFixed()),
        primaryFixedDim = dynamic.paint(colors.primaryFixedDim()),
        onPrimaryFixed = dynamic.paint(colors.onPrimaryFixed()),
        onPrimaryFixedVariant = dynamic.paint(colors.onPrimaryFixedVariant()),

        secondary = dynamic.paint(colors.secondary()),
        onSecondary = dynamic.paint(colors.onSecondary()),
        secondaryContainer = dynamic.paint(colors.secondaryContainer()),
        onSecondaryContainer = dynamic.paint(colors.onSecondaryContainer()),
        secondaryFixed = dynamic.paint(colors.secondaryFixed()),
        secondaryFixedDim = dynamic.paint(colors.secondaryFixedDim()),
        onSecondaryFixed = dynamic.paint(colors.onSecondaryFixed()),
        onSecondaryFixedVariant = dynamic.paint(colors.onSecondaryFixedVariant()),

        tertiary = dynamic.paint(colors.tertiary()),
        onTertiary = dynamic.paint(colors.onTertiary()),
        tertiaryContainer = dynamic.paint(colors.tertiaryContainer()),
        onTertiaryContainer = dynamic.paint(colors.onTertiaryContainer()),
        tertiaryFixed = dynamic.paint(colors.tertiaryFixed()),
        tertiaryFixedDim = dynamic.paint(colors.tertiaryFixedDim()),
        onTertiaryFixed = dynamic.paint(colors.onTertiaryFixed()),
        onTertiaryFixedVariant = dynamic.paint(colors.onTertiaryFixedVariant()),

        error = dynamic.paint(colors.error()),
        onError = dynamic.paint(colors.onError()),
        errorContainer = dynamic.paint(colors.errorContainer()),
        onErrorContainer = dynamic.paint(colors.onErrorContainer()),

        background = dynamic.paint(colors.background()),
        onBackground = dynamic.paint(colors.onBackground()),
        surface = dynamic.paint(colors.surface()),
        onSurface = dynamic.paint(colors.onSurface()),
        surfaceVariant = dynamic.paint(colors.surfaceVariant()),
        onSurfaceVariant = dynamic.paint(colors.onSurfaceVariant()),
        surfaceDim = dynamic.paint(colors.surfaceDim()),
        surfaceBright = dynamic.paint(colors.surfaceBright()),
        surfaceContainerLowest = dynamic.paint(colors.surfaceContainerLowest()),
        surfaceContainerLow = dynamic.paint(colors.surfaceContainerLow()),
        surfaceContainer = dynamic.paint(colors.surfaceContainer()),
        surfaceContainerHigh = dynamic.paint(colors.surfaceContainerHigh()),
        surfaceContainerHighest = dynamic.paint(colors.surfaceContainerHighest()),

        inverseSurface = dynamic.paint(colors.inverseSurface()),
        inverseOnSurface = dynamic.paint(colors.inverseOnSurface()),
        outline = dynamic.paint(colors.outline()),
        outlineVariant = dynamic.paint(colors.outlineVariant()),
        scrim = dynamic.paint(colors.scrim()),
    )
}

private fun DynamicScheme.paint(role: DynamicColor): Color = Color(role.getArgb(this))
