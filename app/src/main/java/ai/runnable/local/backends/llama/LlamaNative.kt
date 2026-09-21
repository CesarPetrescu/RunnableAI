package ai.runnable.local.backends.llama

object LlamaNative {
    init {
        System.loadLibrary("runnableai")
    }

    external fun init(nativeLibDir: String)
    external fun loadModel(path: String, nGpuLayers: Int): Long
    external fun freeModel(handle: Long)
    external fun generate(
        modelHandle: Long,
        prompt: String,
        nCtx: Int,
        nPredict: Int,
        nThreads: Int,
        temperature: Float
    ): String

    external fun generateStream(
        modelHandle: Long,
        prompt: String,
        nCtx: Int,
        nPredict: Int,
        nThreads: Int,
        temperature: Float,
        callback: LlamaStreamCallback
    ): String

    external fun systemInfo(): String
}

interface LlamaStreamCallback {
    fun onToken(token: String)
    fun onStats(
        promptTokens: Int,
        promptMs: Long,
        promptTokensPerSec: Float,
        genTokens: Int,
        genMs: Long,
        genTokensPerSec: Float
    )
    fun onError(message: String)
}
