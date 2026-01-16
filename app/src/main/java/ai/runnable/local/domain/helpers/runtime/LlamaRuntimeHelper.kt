package ai.runnable.local.domain.helpers.runtime

import ai.runnable.local.backends.llama.LlamaBackend
import ai.runnable.local.backends.llama.LlamaParams

class LlamaRuntimeHelper(private val backend: LlamaBackend) {
    suspend fun generate(modelPath: String, prompt: String, params: LlamaParams): String {
        return backend.generate(modelPath, prompt, params)
    }
}
