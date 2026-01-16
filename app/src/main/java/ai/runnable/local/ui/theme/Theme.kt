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
    // Primary: Main actions, buttons, links
    primary = Cobalt500,
    onPrimary = Color.White,
    primaryContainer = Cobalt100,
    onPrimaryContainer = Cobalt700,

    // Secondary: Secondary actions, less prominent elements
    secondary = Cobalt600,
    onSecondary = Color.White,
    secondaryContainer = Cobalt50,
    onSecondaryContainer = Cobalt700,

    // Tertiary: Accents, success states
    tertiary = Emerald500,
    onTertiary = Color.White,
    tertiaryContainer = Emerald100,
    onTertiaryContainer = Emerald700,

    // Background & Surface
    background = Clay50,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Cloud100,
    onSurfaceVariant = Ink500,
    surfaceContainerHighest = Clay100,

    // Error states
    error = Coral500,
    onError = Color.White,
    errorContainer = Coral100,
    onErrorContainer = Coral700,

    // Outline
    outline = Ink300,
    outlineVariant = Ink200
)

// ─────────────────────────────────────────────────────────────────────────────
// Dark Color Scheme
// ─────────────────────────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    // Primary: Main actions, buttons, links
    primary = Cobalt400,
    onPrimary = Ink900,
    primaryContainer = Cobalt700,
    onPrimaryContainer = Cobalt100,

    // Secondary: Secondary actions
    secondary = Cobalt400,
    onSecondary = Ink900,
    secondaryContainer = Cobalt700,
    onSecondaryContainer = Cobalt200,

    // Tertiary: Accents, success states
    tertiary = Emerald400,
    onTertiary = Ink900,
    tertiaryContainer = Emerald700,
    onTertiaryContainer = Emerald100,

    // Background & Surface
    background = Ink900,
    onBackground = Ink100,
    surface = Ink800,
    onSurface = Ink100,
    surfaceVariant = Ink700,
    onSurfaceVariant = Ink300,
    surfaceContainerHighest = Ink700,

    // Error states
    error = Coral400,
    onError = Ink900,
    errorContainer = Coral700,
    onErrorContainer = Coral100,

    // Outline
    outline = Ink500,
    outlineVariant = Ink600
)

// ─────────────────────────────────────────────────────────────────────────────
// Extended Colors (for badges and semantic UI)
// ─────────────────────────────────────────────────────────────────────────────
data class RunnableColors(
    val isDark: Boolean,
    // Task badge colors
    val chatBadgeBg: Color,
    val chatBadgeFg: Color,
    val asrBadgeBg: Color,
    val asrBadgeFg: Color,
    val ttsBadgeBg: Color,
    val ttsBadgeFg: Color,
    val codecBadgeBg: Color,
    val codecBadgeFg: Color,
    // Runtime badge colors
    val llamaBadgeBg: Color,
    val llamaBadgeFg: Color,
    val onnxBadgeBg: Color,
    val onnxBadgeFg: Color,
    val execuTorchBadgeBg: Color,
    val execuTorchBadgeFg: Color,
    // Chip colors
    val chipBg: Color,
    val chipFg: Color
)

private val LightRunnableColors = RunnableColors(
    isDark = false,
    chatBadgeBg = BadgeColors.chatBg,
    chatBadgeFg = BadgeColors.chatFg,
    asrBadgeBg = BadgeColors.asrBg,
    asrBadgeFg = BadgeColors.asrFg,
    ttsBadgeBg = BadgeColors.ttsBg,
    ttsBadgeFg = BadgeColors.ttsFg,
    codecBadgeBg = BadgeColors.codecBg,
    codecBadgeFg = BadgeColors.codecFg,
    llamaBadgeBg = BadgeColors.llamaBg,
    llamaBadgeFg = BadgeColors.llamaFg,
    onnxBadgeBg = BadgeColors.onnxBg,
    onnxBadgeFg = BadgeColors.onnxFg,
    execuTorchBadgeBg = BadgeColors.execuTorchBg,
    execuTorchBadgeFg = BadgeColors.execuTorchFg,
    chipBg = Cobalt100,
    chipFg = Cobalt700
)

private val DarkRunnableColors = RunnableColors(
    isDark = true,
    chatBadgeBg = BadgeColors.chatBgDark,
    chatBadgeFg = BadgeColors.chatFgDark,
    asrBadgeBg = BadgeColors.asrBgDark,
    asrBadgeFg = BadgeColors.asrFgDark,
    ttsBadgeBg = BadgeColors.ttsBgDark,
    ttsBadgeFg = BadgeColors.ttsFgDark,
    codecBadgeBg = BadgeColors.codecBgDark,
    codecBadgeFg = BadgeColors.codecFgDark,
    llamaBadgeBg = BadgeColors.llamaBgDark,
    llamaBadgeFg = BadgeColors.llamaFgDark,
    onnxBadgeBg = BadgeColors.onnxBgDark,
    onnxBadgeFg = BadgeColors.onnxFgDark,
    execuTorchBadgeBg = BadgeColors.execuTorchBgDark,
    execuTorchBadgeFg = BadgeColors.execuTorchFgDark,
    chipBg = Cobalt700,
    chipFg = Cobalt100
)

val LocalRunnableColors = staticCompositionLocalOf { LightRunnableColors }

// ─────────────────────────────────────────────────────────────────────────────
// Theme Composable
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

// Extension to access custom colors
object RunnableTheme {
    val colors: RunnableColors
        @Composable
        get() = LocalRunnableColors.current
}
