package ai.runnable.local.ui.components

import ai.runnable.local.ui.theme.RunnableTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    val isDark = RunnableTheme.colors.isDark
    val colorScheme = MaterialTheme.colorScheme
    val accent = RunnableTheme.colors.accent

    val transition = rememberInfiniteTransition(label = "gradient")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )

    val gradient = Brush.linearGradient(
        colors = listOf(
            colorScheme.background,
            if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.3f) else accent.copy(alpha = 0.08f),
            colorScheme.background
        ),
        start = Offset(0f, 200f * shift),
        end = Offset(800f, 800f * (1f - shift))
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        // Subtle accent blob
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-80).dp)
                .blur(100.dp)
                .background(accent.copy(alpha = if (isDark) 0.06f else 0.12f))
        )
        // Secondary accent blob
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 200.dp, y = 500.dp)
                .blur(120.dp)
                .background(accent.copy(alpha = if (isDark) 0.04f else 0.08f))
        )
        content()
    }
}
