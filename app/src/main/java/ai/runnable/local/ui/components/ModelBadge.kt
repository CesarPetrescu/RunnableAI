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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ModelBadge(
    task: ModelTask,
    runtime: RuntimeType,
    modifier: Modifier = Modifier
) {
    val colors = RunnableTheme.colors
    
    Row(modifier = modifier) {
        Badge(
            text = task.name,
            background = taskBackground(task, colors),
            foreground = taskForeground(task, colors)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Badge(
            text = runtimeLabel(runtime),
            background = runtimeBackground(runtime, colors),
            foreground = runtimeForeground(runtime, colors)
        )
    }
}

@Composable
private fun Badge(
    text: String,
    background: Color,
    foreground: Color
) {
    Surface(
        color = background,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun runtimeLabel(runtime: RuntimeType): String {
    return when (runtime) {
        RuntimeType.LLAMA_CPP -> "llama.cpp"
        RuntimeType.ONNX -> "ONNX"
        RuntimeType.EXECUTORCH -> "ExecuTorch"
    }
}

private fun taskBackground(task: ModelTask, colors: ai.runnable.local.ui.theme.RunnableColors): Color {
    return when (task) {
        ModelTask.CHAT -> colors.chatBadgeBg
        ModelTask.ASR -> colors.asrBadgeBg
        ModelTask.TTS -> colors.ttsBadgeBg
        ModelTask.CODEC -> colors.codecBadgeBg
    }
}

private fun taskForeground(task: ModelTask, colors: ai.runnable.local.ui.theme.RunnableColors): Color {
    return when (task) {
        ModelTask.CHAT -> colors.chatBadgeFg
        ModelTask.ASR -> colors.asrBadgeFg
        ModelTask.TTS -> colors.ttsBadgeFg
        ModelTask.CODEC -> colors.codecBadgeFg
    }
}

private fun runtimeBackground(runtime: RuntimeType, colors: ai.runnable.local.ui.theme.RunnableColors): Color {
    return when (runtime) {
        RuntimeType.LLAMA_CPP -> colors.llamaBadgeBg
        RuntimeType.ONNX -> colors.onnxBadgeBg
        RuntimeType.EXECUTORCH -> colors.execuTorchBadgeBg
    }
}

private fun runtimeForeground(runtime: RuntimeType, colors: ai.runnable.local.ui.theme.RunnableColors): Color {
    return when (runtime) {
        RuntimeType.LLAMA_CPP -> colors.llamaBadgeFg
        RuntimeType.ONNX -> colors.onnxBadgeFg
        RuntimeType.EXECUTORCH -> colors.execuTorchBadgeFg
    }
}
