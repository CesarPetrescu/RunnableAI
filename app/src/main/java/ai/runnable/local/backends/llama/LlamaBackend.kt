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
