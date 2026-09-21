package ai.runnable.local.domain.helpers.runtime

import ai.runnable.local.backends.llama.LlamaBackend
import ai.runnable.local.backends.llama.LlamaParams
import ai.runnable.local.backends.llama.LlamaPerfStats

class LlamaRuntimeHelper(private val backend: LlamaBackend) {
    suspend fun generate(modelPath: String, prompt: String, params: LlamaParams): String {
        return backend.generate(modelPath, prompt, params)
    }

    suspend fun generateStream(
        modelPath: String,
        prompt: String,
        params: LlamaParams,
        onToken: (String) -> Unit,
        onStats: (LlamaPerfStats) -> Unit
    ): String {
        return backend.generateStream(modelPath, prompt, params, onToken, onStats)
    }
}
