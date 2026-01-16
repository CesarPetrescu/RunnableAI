package ai.runnable.local.ui.components

import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RuntimeType
import ai.runnable.local.ui.theme.Cobalt100
import ai.runnable.local.ui.theme.Cobalt500
import ai.runnable.local.ui.theme.Coral100
import ai.runnable.local.ui.theme.Coral500
import ai.runnable.local.ui.theme.Mint100
import ai.runnable.local.ui.theme.Mint500
import ai.runnable.local.ui.theme.Sun100
import ai.runnable.local.ui.theme.Sun500
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
fun ModelBadge(task: ModelTask, runtime: RuntimeType) {
    Row {
        Badge(text = task.name, background = taskBackground(task), content = taskForeground(task))
        Spacer(modifier = Modifier.width(8.dp))
        Badge(text = runtime.name, background = runtimeBackground(runtime), content = runtimeForeground(runtime))
    }
}

@Composable
private fun Badge(text: String, background: Color, content: Color) {
    Surface(color = background, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun runtimeBackground(runtime: RuntimeType): Color {
    return when (runtime) {
        RuntimeType.LLAMA_CPP -> Cobalt100
        RuntimeType.ONNX -> Mint100
        RuntimeType.EXECUTORCH -> Coral100
    }
}

private fun runtimeForeground(runtime: RuntimeType): Color {
    return when (runtime) {
        RuntimeType.LLAMA_CPP -> Cobalt500
        RuntimeType.ONNX -> Mint500
        RuntimeType.EXECUTORCH -> Coral500
    }
}

private fun taskBackground(task: ModelTask): Color {
    return when (task) {
        ModelTask.CHAT -> Cobalt100
        ModelTask.ASR -> Mint100
        ModelTask.TTS -> Sun100
        ModelTask.CODEC -> Coral100
    }
}

private fun taskForeground(task: ModelTask): Color {
    return when (task) {
        ModelTask.CHAT -> Cobalt500
        ModelTask.ASR -> Mint500
        ModelTask.TTS -> Sun500
        ModelTask.CODEC -> Coral500
    }
}
