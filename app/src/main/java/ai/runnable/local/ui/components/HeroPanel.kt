package ai.runnable.local.ui.components

import ai.runnable.local.ui.theme.RunnableTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HeroPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = RunnableTheme.colors.accent
    val isDark = RunnableTheme.colors.isDark

    val gradient = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = if (isDark) 0.15f else 0.25f),
            Color.Transparent
        )
    )

    Surface(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
