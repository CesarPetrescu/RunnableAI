package ai.runnable.local.backends.llama

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class LlamaBackend(private val context: Context) {
    private val initialized = AtomicBoolean(false)

    private fun ensureInit() {
        if (initialized.compareAndSet(false, true)) {
            val nativeDir = context.applicationInfo.nativeLibraryDir
            LlamaNative.init(nativeDir)
        }
    }

    suspend fun generate(modelPath: String, prompt: String, params: LlamaParams): String =
        withContext(Dispatchers.IO) {
            ensureInit()
            val handle = LlamaNative.loadModel(modelPath, params.nGpuLayers)
            if (handle == 0L) {
                throw IllegalStateException("Failed to load model: $modelPath")
            }
            try {
                LlamaNative.generate(
                    modelHandle = handle,
                    prompt = prompt,
                    nCtx = params.nCtx,
                    nPredict = params.nPredict,
                    nThreads = params.nThreads,
                    temperature = params.temperature
                )
            } finally {
                LlamaNative.freeModel(handle)
            }
        }

    suspend fun generateStream(
        modelPath: String,
        prompt: String,
        params: LlamaParams,
        onToken: (String) -> Unit,
        onStats: (LlamaPerfStats) -> Unit
    ): String = withContext(Dispatchers.IO) {
        ensureInit()
        val handle = LlamaNative.loadModel(modelPath, params.nGpuLayers)
        if (handle == 0L) {
            throw IllegalStateException("Failed to load model: $modelPath")
        }
        try {
            var errorMessage: String? = null
            val callback = object : LlamaStreamCallback {
                override fun onToken(token: String) {
                    onToken(token)
                }

                override fun onStats(
                    promptTokens: Int,
                    promptMs: Long,
                    promptTokensPerSec: Float,
                    genTokens: Int,
                    genMs: Long,
                    genTokensPerSec: Float
                ) {
                    onStats(
                        LlamaPerfStats(
                            promptTokens = promptTokens,
                            promptMs = promptMs,
                            promptTokensPerSec = promptTokensPerSec,
                            genTokens = genTokens,
                            genMs = genMs,
                            genTokensPerSec = genTokensPerSec
                        )
                    )
                }

                override fun onError(message: String) {
                    errorMessage = message
                }
            }
            val output = LlamaNative.generateStream(
                modelHandle = handle,
                prompt = prompt,
                nCtx = params.nCtx,
                nPredict = params.nPredict,
                nThreads = params.nThreads,
                temperature = params.temperature,
                callback = callback
            )
            if (errorMessage != null) {
                throw IllegalStateException(errorMessage)
            }
            output
        } finally {
            LlamaNative.freeModel(handle)
        }
    }

    fun systemInfo(): String {
        ensureInit()
        return LlamaNative.systemInfo()
    }
}

data class LlamaParams(
    val nCtx: Int = 2048,
    val nPredict: Int = 128,
    val nThreads: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(2),
    val nGpuLayers: Int = 0,
    val temperature: Float = 0.7f
)

data class LlamaPerfStats(
    val promptTokens: Int,
    val promptMs: Long,
    val promptTokensPerSec: Float,
    val genTokens: Int,
    val genMs: Long,
    val genTokensPerSec: Float
)
