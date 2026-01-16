package ai.runnable.local.ui.components

import ai.runnable.local.ui.theme.RunnableTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TokenStreamBar(text: String, modifier: Modifier = Modifier) {
    val colors = RunnableTheme.colors
    val tokens = text.split(" ")
        .filter { it.isNotBlank() }
        .take(24)

    if (tokens.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(tokens) { token ->
            Surface(
                color = colors.chipBg,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = token,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.chipFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
