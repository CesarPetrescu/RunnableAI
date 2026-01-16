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
// Light Color Scheme
// ─────────────────────────────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary = Steel,
    onPrimary = TextOnSteel,
    primaryContainer = SteelLight,
    onPrimaryContainer = TextOnSteel,
    secondary = Lemon,
    onSecondary = TextOnLemon,
    secondaryContainer = LemonMuted,
    onSecondaryContainer = TextOnLemon,
    tertiary = Lemon,
    onTertiary = TextOnLemon,
    tertiaryContainer = LemonLight,
    onTertiaryContainer = TextOnLemon,
    background = LightBg,
    onBackground = Steel,
    surface = LightSurface,
    onSurface = Steel,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = SteelLighter,
    surfaceContainerHighest = LightSurfaceAlt,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = ErrorDark,
    outline = SteelMuted,
    outlineVariant = SteelSubtle
)

// ─────────────────────────────────────────────────────────────────────────────
// Dark Color Scheme
// ─────────────────────────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary = Lemon,
    onPrimary = TextOnLemon,
    primaryContainer = LemonDark,
    onPrimaryContainer = TextOnLemon,
    secondary = Lemon,
    onSecondary = TextOnLemon,
    secondaryContainer = SteelLight,
    onSecondaryContainer = TextOnSteel,
    tertiary = LemonLight,
    onTertiary = TextOnLemon,
    tertiaryContainer = SteelLight,
    onTertiaryContainer = Lemon,
    background = SteelDarker,
    onBackground = TextOnSteel,
    surface = Steel,
    onSurface = TextOnSteel,
    surfaceVariant = SteelLight,
    onSurfaceVariant = TextMutedDark,
    surfaceContainerHighest = SteelSurface,
    error = Error,
    onError = SteelDarker,
    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = Error,
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
    onAccent = TextOnLemon,
    chipBg = LemonMuted,
    chipFg = Steel,
    badgeBg = Steel,
    badgeFg = TextOnSteel,
    success = SuccessDark,
    successBg = Color(0xFFDCFCE7)
)

private val DarkRunnableColors = RunnableColors(
    isDark = true,
    accent = Lemon,
    accentMuted = LemonDark,
    onAccent = TextOnLemon,
    chipBg = SteelSurface,
    chipFg = Lemon,
    badgeBg = Lemon,
    badgeFg = TextOnLemon,
    success = Success,
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
