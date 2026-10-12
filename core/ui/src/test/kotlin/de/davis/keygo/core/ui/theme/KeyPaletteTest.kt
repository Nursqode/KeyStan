package de.davis.keygo.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The palette engine is what stands between the user's choice and every colour on screen, so these
 * pin what a user can check by eye — a palette called red has to look red, surfaces have to stay
 * close to grey, text on an accent has to stay legible — instead of the exact hex the generator
 * happens to emit today.
 */
class KeyPaletteTest {

    @Test
    fun `the system palette keeps deciding for itself`() {
        // No scheme of its own is what sends the theme to the wallpaper, or to the violet the fork
        // inherited: either way the setting cannot change the look of somebody who never used it.
        for (isDark in MODES) {
            assertNull(KeyPalette.System.scheme(isDark, contrastLevel = 0.0))
        }
    }

    @Test
    fun `red is red`() {
        // The bug this replaces: rotating the inherited violet scheme by hue kept its 25% saturation,
        // so "red" came out a muted brown (#80514D) that no one would call red.
        val pureRed = Hct.fromInt(0xFFFF0000.toInt())

        for (isDark in MODES) {
            val accent = Hct.fromInt(redScheme(isDark).primary.toArgb())

            assertTrue(
                abs(accent.hue - pureRed.hue) < HUE_TOLERANCE,
                "red is ${accent.hue} degrees, ${pureRed.hue} away from being red",
            )
            assertTrue(
                accent.chroma >= ACCENT_CHROMA,
                "red is washed out at chroma ${accent.chroma}",
            )
        }
    }

    @Test
    fun `red stays red in the other accent families`() {
        // Secondary is the same hue at a lower chroma, so the interface reads as one palette rather
        // than as a red primary sitting next to somebody else's colours.
        val pureRed = Hct.fromInt(0xFFFF0000.toInt())

        for (isDark in MODES) {
            val scheme = redScheme(isDark)

            listOf(scheme.primary, scheme.secondary, scheme.primaryContainer).forEach { color ->
                val accent = Hct.fromInt(color.toArgb())
                assertTrue(
                    abs(accent.hue - pureRed.hue) < HUE_TOLERANCE,
                    "$color is ${accent.hue} degrees, which is not a red",
                )
            }
        }
    }

    @Test
    fun `a generated palette is a complete scheme`() {
        for (isDark in MODES) {
            val scheme = redScheme(isDark)

            roles.forEach { role -> assertTrue(role(scheme).isSpecified, "a role is unspecified") }
        }
    }

    @Test
    fun `a generated palette keeps its surfaces close to grey`() {
        // A surface carrying the accent is how a palette makes text unreadable. Material's neutrals
        // sit in single digits and its neutral-variant roles can reach the mid teens, while a surface
        // that actually took the accent would be at the accent's own chroma — 76 for this red.
        for (isDark in MODES) {
            val scheme = redScheme(isDark)

            surfaces.forEach { role ->
                val chroma = Hct.fromInt(role(scheme).toArgb()).chroma
                assertTrue(chroma <= NEUTRAL_CHROMA, "a surface is tinted at chroma $chroma")
            }
        }
    }

    @Test
    fun `text on an accent stays legible`() {
        for (isDark in MODES) {
            val scheme = redScheme(isDark)

            assertTrue(contrast(scheme.primary, scheme.onPrimary) >= MIN_TEXT_CONTRAST, "onPrimary")
            assertTrue(
                contrast(scheme.primaryContainer, scheme.onPrimaryContainer) >= MIN_TEXT_CONTRAST,
                "onPrimaryContainer",
            )
            assertTrue(contrast(scheme.surface, scheme.onSurface) >= MIN_TEXT_CONTRAST, "onSurface")
        }
    }

    @Test
    fun `a higher contrast level raises the contrast`() {
        for (isDark in MODES) {
            val standard = redScheme(isDark, contrastLevel = 0.0)
            val high = redScheme(isDark, contrastLevel = 1.0)

            assertTrue(
                contrast(high.surface, high.onSurface) > contrast(standard.surface, standard.onSurface),
                "the device asked for more contrast and got the same interface",
            )
        }
    }

    @Test
    fun `system mode follows the device and the fixed modes ignore it`() {
        assertTrue(ThemeMode.System.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.System.isDark(systemInDarkTheme = false))

        assertFalse(ThemeMode.Light.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.Light.isDark(systemInDarkTheme = false))

        assertTrue(ThemeMode.Dark.isDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.Dark.isDark(systemInDarkTheme = false))
    }

    private fun redScheme(isDark: Boolean, contrastLevel: Double = 0.0): ColorScheme =
        checkNotNull(KeyPalette.Red.scheme(isDark, contrastLevel)) { "red has no scheme" }

    /** WCAG relative-luminance ratio, which is what Material's contrast levels are built on. */
    private fun contrast(background: Color, foreground: Color): Float {
        val brighter = max(background.luminance(), foreground.luminance())
        val darker = min(background.luminance(), foreground.luminance())
        return (brighter + 0.05f) / (darker + 0.05f)
    }

    /** Every role the app draws with, on and around the accents. */
    private val roles = listOf<(ColorScheme) -> Color>(
        { it.primary },
        { it.onPrimary },
        { it.primaryContainer },
        { it.onPrimaryContainer },
        { it.inversePrimary },
        { it.primaryFixed },
        { it.onPrimaryFixed },
        { it.secondary },
        { it.onSecondary },
        { it.secondaryContainer },
        { it.onSecondaryContainer },
        { it.tertiary },
        { it.onTertiary },
        { it.tertiaryContainer },
        { it.onTertiaryContainer },
        { it.error },
        { it.onError },
        { it.background },
        { it.onBackground },
        { it.surface },
        { it.onSurface },
        { it.outline },
        { it.outlineVariant },
        { it.scrim },
    )

    /** The roles that must stay quiet for text to remain readable. */
    private val surfaces = listOf<(ColorScheme) -> Color>(
        { it.background },
        { it.surface },
        { it.surfaceVariant },
        { it.surfaceContainerLowest },
        { it.surfaceContainerLow },
        { it.surfaceContainer },
        { it.surfaceContainerHigh },
        { it.surfaceContainerHighest },
    )

    private companion object {
        val MODES = listOf(false, true)

        /** HCT hue is measured in degrees; a seed palette sits within a couple of its own. */
        const val HUE_TOLERANCE = 25.0

        /** The muted brown this bug produced sat at chroma ~17, so anything above 25 reads as a colour. */
        const val ACCENT_CHROMA = 25.0

        /** Material's neutrals are single-digit; this leaves room without hiding an accent in them. */
        const val NEUTRAL_CHROMA = 20.0

        /** WCAG AA for body text. */
        const val MIN_TEXT_CONTRAST = 4.5f
    }
}
