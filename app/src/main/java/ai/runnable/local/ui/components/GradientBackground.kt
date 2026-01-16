package ai.runnable.local.ui.components

import ai.runnable.local.ui.theme.RunnableTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    val isDark = RunnableTheme.colors.isDark
    val colorScheme = MaterialTheme.colorScheme

    val transition = rememberInfiniteTransition(label = "gradient")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )

    // Theme-aware gradient colors
    val gradientColors = if (isDark) {
        listOf(
            colorScheme.background,
            colorScheme.surfaceVariant.copy(alpha = 0.5f),
            colorScheme.background
        )
    } else {
        listOf(
            colorScheme.background,
            colorScheme.primaryContainer.copy(alpha = 0.3f),
            colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        )
    }

    val gradient = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(0f, 200f * shift),
        end = Offset(800f, 800f * (1f - shift))
    )

    // Blob colors - theme aware
    val blobColors = if (isDark) {
        Triple(
            colorScheme.primary.copy(alpha = 0.08f),
            colorScheme.tertiary.copy(alpha = 0.06f),
            colorScheme.secondary.copy(alpha = 0.05f)
        )
    } else {
        Triple(
            colorScheme.primary.copy(alpha = 0.12f),
            colorScheme.tertiary.copy(alpha = 0.10f),
            colorScheme.secondary.copy(alpha = 0.08f)
        )
    }

    val dotColor = if (isDark) {
        colorScheme.onBackground.copy(alpha = 0.04f)
    } else {
        colorScheme.onBackground.copy(alpha = 0.06f)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        // Decorative blurred blobs
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-40).dp)
                .blur(60.dp)
                .background(blobColors.first)
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 180.dp, y = 420.dp)
                .blur(70.dp)
                .background(blobColors.second)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = 40.dp, y = 140.dp)
                .blur(80.dp)
                .background(blobColors.third)
        )
        
        DotGridOverlay(
            step = 28.dp,
            dotRadius = 1.2.dp,
            tint = dotColor
        )
        
        content()
    }
}

@Composable
private fun DotGridOverlay(step: Dp, dotRadius: Dp, tint: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stepPx = step.toPx()
        val radiusPx = dotRadius.toPx()
        val width = size.width
        val height = size.height
        var y = stepPx * 0.6f
        while (y < height) {
            var x = stepPx * 0.4f
            while (x < width) {
                drawCircle(
                    color = tint,
                    radius = radiusPx,
                    center = Offset(x, y),
                    style = Fill
                )
                x += stepPx
            }
            y += stepPx
        }
    }
}
