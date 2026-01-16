package ai.runnable.local.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Light Color Scheme - Steel primary, dark text on light backgrounds
// ─────────────────────────────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary = Steel,
    onPrimary = TextLight,
    primaryContainer = SteelLight,
    onPrimaryContainer = TextLight,
    secondary = Steel,
    onSecondary = TextLight,
    secondaryContainer = LemonMuted,
    onSecondaryContainer = TextDark,
    tertiary = Lemon,
    onTertiary = TextDark,
    tertiaryContainer = LemonLight,
    onTertiaryContainer = TextDark,
    background = LightBg,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = TextDarkMuted,
    surfaceContainerHighest = LightSurfaceAlt,
    error = Error,
    onError = TextLight,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFFB91C1C),
    outline = SteelMuted,
    outlineVariant = SteelSubtle
)

// ─────────────────────────────────────────────────────────────────────────────
// Dark Color Scheme - Lemon accent, light text on dark backgrounds
// ─────────────────────────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary = Lemon,
    onPrimary = TextDark,
    primaryContainer = LemonDark,
    onPrimaryContainer = TextDark,
    secondary = Lemon,
    onSecondary = TextDark,
    secondaryContainer = SteelLight,
    onSecondaryContainer = TextLight,
    tertiary = LemonLight,
    onTertiary = TextDark,
    tertiaryContainer = SteelLight,
    onTertiaryContainer = Lemon,
    background = SteelDarker,
    onBackground = TextLight,
    surface = Steel,
    onSurface = TextLight,
    surfaceVariant = SteelLight,
    onSurfaceVariant = TextLightMuted,
    surfaceContainerHighest = SteelSurface,
    error = ErrorLight,
    onError = TextDark,
    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = ErrorLight,
    outline = SteelMuted,
    outlineVariant = SteelLighter
)

// ─────────────────────────────────────────────────────────────────────────────
// Extended Colors
// ─────────────────────────────────────────────────────────────────────────────
data class RunnableColors(
    val isDark: Boolean,
    val accent: Color,
    val accentMuted: Color,
    val onAccent: Color,
    val chipBg: Color,
    val chipFg: Color,
    val badgeBg: Color,
    val badgeFg: Color,
    val success: Color,
    val successBg: Color
)

private val LightRunnableColors = RunnableColors(
    isDark = false,
    accent = Lemon,
    accentMuted = LemonMuted,
    onAccent = TextDark,
    chipBg = LemonMuted,
    chipFg = TextDark,
    badgeBg = Steel,
    badgeFg = TextLight,
    success = Success,
    successBg = Color(0xFFDCFCE7)
)

private val DarkRunnableColors = RunnableColors(
    isDark = true,
    accent = Lemon,
    accentMuted = LemonDark,
    onAccent = TextDark,
    chipBg = SteelSurface,
    chipFg = TextLight,
    badgeBg = Lemon,
    badgeFg = TextDark,
    success = SuccessLight,
    successBg = Color(0xFF1A3D2A)
)

val LocalRunnableColors = staticCompositionLocalOf { LightRunnableColors }

// ─────────────────────────────────────────────────────────────────────────────
// Theme
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RunnableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val runnableColors = if (darkTheme) DarkRunnableColors else LightRunnableColors

    CompositionLocalProvider(LocalRunnableColors provides runnableColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = RunnableTypography,
            shapes = RunnableShapes,
            content = content
        )
    }
}

object RunnableTheme {
    val colors: RunnableColors
        @Composable
        get() = LocalRunnableColors.current
}
