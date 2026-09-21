package ai.runnable.local.domain.helpers

import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RuntimeType
import ai.runnable.local.asr.CanaryAsrEngine
import ai.runnable.local.asr.CanaryConfig
import ai.runnable.local.audio.WavReader
import ai.runnable.local.domain.helpers.runtime.OnnxRuntimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AsrHelper(
    private val resolver: ModelResolver,
    private val onnxRuntime: OnnxRuntimeHelper
) {
    suspend fun recognize(modelId: String, audioFile: File): Result<String> {
        return when (val resolved = resolver.resolveReady(modelId)) {
            is ResolveResult.Error -> Result.failure(IllegalStateException(resolved.message))
            is ResolveResult.Success -> {
                val model = resolved.model
                if (model.task != ModelTask.ASR || model.runtime != RuntimeType.ONNX) {
                    return Result.failure(IllegalStateException("Model is not an ONNX ASR model"))
                }
                val files = resolved.files
                val encoder = files.firstOrNull {
                    it.name.contains("encoder", ignoreCase = true) && it.name.endsWith(".onnx")
                } ?: return Result.failure(IllegalStateException("Missing encoder ONNX file"))
                val decoder = files.firstOrNull {
                    it.name.contains("decoder", ignoreCase = true) && it.name.endsWith(".onnx")
                } ?: return Result.failure(IllegalStateException("Missing decoder ONNX file"))
                val vocab = files.firstOrNull { it.name.equals("vocab.txt", ignoreCase = true) }
                    ?: return Result.failure(IllegalStateException("Missing vocab.txt"))
                val configFile = files.firstOrNull { it.name.equals("config.json", ignoreCase = true) }

                withContext(Dispatchers.Default) {
                    runCatching {
                        val wav = WavReader.readPcm16(audioFile)
                        val config = CanaryConfig.load(configFile)
                        val engine = CanaryAsrEngine(
                            encoderPath = encoder.absolutePath,
                            decoderPath = decoder.absolutePath,
                            vocabPath = vocab.absolutePath,
                            config = config
                        )
                        try {
                            engine.transcribe(wav.samples, wav.sampleRate)
                        } finally {
                            engine.close()
                        }
                    }
                }
            }
        }
    }
}
