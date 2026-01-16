package ai.runnable.local.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
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

    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF7F2EA),
            Color(0xFFE8F0FF),
            Color(0xFFFFF1DD)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 200f * shift),
        end = androidx.compose.ui.geometry.Offset(800f, 800f * (1f - shift))
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-40).dp)
                .blur(60.dp)
                .background(Color(0x33FFB347))
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 180.dp, y = 420.dp)
                .blur(70.dp)
                .background(Color(0x3321B58B))
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = 40.dp, y = 140.dp)
                .blur(80.dp)
                .background(Color(0x331C4ED8))
        )
        DotGridOverlay(
            step = 28.dp,
            dotRadius = 1.2.dp,
            tint = Color(0x14000000)
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
