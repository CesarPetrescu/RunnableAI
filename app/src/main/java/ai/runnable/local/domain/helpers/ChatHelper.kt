package ai.runnable.local.domain.helpers

import ai.runnable.local.backends.llama.LlamaParams
import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RuntimeType
import ai.runnable.local.domain.ChatResult
import ai.runnable.local.domain.helpers.runtime.LlamaRuntimeHelper

class ChatHelper(
    private val resolver: ModelResolver,
    private val llamaRuntime: LlamaRuntimeHelper
) {
    suspend fun run(modelId: String, prompt: String): ChatResult {
        return when (val resolved = resolver.resolveReady(modelId)) {
            is ResolveResult.Error -> ChatResult.Error(resolved.message)
            is ResolveResult.Success -> {
                val model = resolved.model
                if (model.task != ModelTask.CHAT || model.runtime != RuntimeType.LLAMA_CPP) {
                    return ChatResult.Error("Model is not a chat-capable llama.cpp model")
                }
                val modelPath = resolved.files.first().absolutePath
                return try {
                    val params = LlamaParams()
                    val text = llamaRuntime.generate(modelPath, prompt, params)
                    ChatResult.Success(text)
                } catch (e: Exception) {
                    ChatResult.Error(e.message ?: "Chat failed")
                }
            }
        }
    }
}
