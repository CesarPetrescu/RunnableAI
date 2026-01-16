package ai.runnable.local.ui.components

import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RuntimeType
import ai.runnable.local.ui.theme.RunnableTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModelBadge(
    task: ModelTask,
    runtime: RuntimeType,
    modifier: Modifier = Modifier
) {
    val colors = RunnableTheme.colors

    Row(modifier = modifier) {
        // Task badge
        Surface(
            color = colors.badgeBg,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.badgeFg,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Runtime badge (inverted)
        Surface(
            color = colors.accentMuted,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = runtimeLabel(runtime),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onAccent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private fun runtimeLabel(runtime: RuntimeType): String {
    return when (runtime) {
        RuntimeType.LLAMA_CPP -> "llama.cpp"
        RuntimeType.ONNX -> "ONNX"
        RuntimeType.EXECUTORCH -> "ExecuTorch"
    }
}
