package de.davis.keygo.core.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext


@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun isContrastAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}

/**
 * The device's contrast preference, or 0 where the platform has no such setting. Material's contrast
 * level is the same 0..1 scale, so a generated palette can be drawn at the contrast the user asked
 * for rather than snapped onto one of three tables.
 */
internal fun Context.contrastLevel(): Float = if (isContrastAvailable()) {
    (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).contrast
} else 0f

@Composable
fun selectSchemeForContrast(isDark: Boolean): ColorScheme {
    val contrastLevel = LocalContext.current.contrastLevel()

    return when {
        contrastLevel <= 0.33f -> if (isDark) darkScheme else lightScheme

        contrastLevel <= 0.66f ->
            if (isDark) mediumContrastDarkColorScheme else mediumContrastLightColorScheme

        else -> if (isDark) highContrastDarkColorScheme else highContrastLightColorScheme
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KeyGoTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    palette: KeyPalette = KeyPalette.System,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = themeColorScheme(isDarkMode, dynamicColor, palette),
        typography = KeyGoTypography,
        content = content
    )
}

/**
 * The scheme a set of theme choices resolves to. Public so that a picker can show what a palette
 * looks like without drawing a second theme around each sample: the sample and the interface then
 * cannot disagree, because there is one decision here and not two.
 */
@Composable
fun themeColorScheme(
    isDarkMode: Boolean,
    dynamicColor: Boolean,
    palette: KeyPalette,
): ColorScheme {
    val context = LocalContext.current

    // A palette the user picked beats the wallpaper: otherwise Material You repaints every colour
    // they did not ask for.
    return palette.scheme(isDarkMode, context.contrastLevel().toDouble()) ?: when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> selectSchemeForContrast(isDarkMode)
    }
}
