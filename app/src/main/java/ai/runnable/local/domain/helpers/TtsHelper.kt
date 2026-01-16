package ai.runnable.local.domain.helpers

import ai.runnable.local.audio.AudioPlayer
import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RuntimeType
import ai.runnable.local.domain.TtsResult
import ai.runnable.local.domain.helpers.runtime.ExecuTorchRuntimeHelper
import ai.runnable.local.domain.helpers.runtime.LlamaRuntimeHelper
import ai.runnable.local.domain.helpers.runtime.OnnxRuntimeHelper

class TtsHelper(
    private val resolver: ModelResolver,
    private val audioPlayer: AudioPlayer,
    private val llamaRuntime: LlamaRuntimeHelper,
    private val onnxRuntime: OnnxRuntimeHelper,
    private val execuTorchRuntime: ExecuTorchRuntimeHelper
) {
    suspend fun synthesize(modelId: String, text: String): TtsResult {
        return when (val resolved = resolver.resolveReady(modelId)) {
            is ResolveResult.Error -> TtsResult.Error(resolved.message)
            is ResolveResult.Success -> {
                val model = resolved.model
                if (model.task != ModelTask.TTS) {
                    return TtsResult.Error("Selected model is not a TTS model")
                }
                return when (model.runtime) {
                    RuntimeType.LLAMA_CPP -> playPlaceholder(text)
                    RuntimeType.ONNX -> playPlaceholder(text)
                    RuntimeType.EXECUTORCH -> playPlaceholder(text)
                }
            }
        }
    }

    private fun playPlaceholder(text: String): TtsResult {
        val durationMs = (text.length.coerceAtLeast(12) * 30).coerceAtMost(2000)
        val samples = audioPlayer.playSineWave(durationMs = durationMs, frequencyHz = 220f)
        return TtsResult.Success(samples)
    }
}
