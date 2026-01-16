package ai.runnable.local.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Cobalt500,
    onPrimary = Color.White,
    secondary = Sun500,
    onSecondary = Ink900,
    tertiary = Mint500,
    onTertiary = Color.White,
    background = Clay50,
    onBackground = Ink900,
    surface = Color(0xFFFCFBF9),
    onSurface = Ink900,
    surfaceVariant = Cloud50,
    onSurfaceVariant = Ink700,
    error = Coral500
)

private val DarkColors = darkColorScheme(
    primary = Cobalt700,
    onPrimary = Color.White,
    secondary = Sun700,
    onSecondary = Ink900,
    tertiary = Mint700,
    onTertiary = Color.White,
    background = Ink900,
    onBackground = Color.White,
    surface = Ink700,
    onSurface = Color.White,
    surfaceVariant = Ink700,
    onSurfaceVariant = Color.White,
    error = Coral500
)

@Composable
fun RunnableTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = RunnableTypography,
        shapes = RunnableShapes,
        content = content
    )
}
