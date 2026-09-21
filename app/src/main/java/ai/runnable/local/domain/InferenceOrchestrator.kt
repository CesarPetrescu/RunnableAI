package ai.runnable.local.domain

import ai.runnable.local.domain.helpers.AsrHelper
import ai.runnable.local.domain.helpers.ChatHelper
import ai.runnable.local.domain.helpers.ChatPerfStats
import ai.runnable.local.domain.helpers.TtsHelper
import java.io.File

class InferenceOrchestrator(
    private val chatHelper: ChatHelper,
    private val ttsHelper: TtsHelper,
    private val asrHelper: AsrHelper
) {
    suspend fun runChat(modelId: String, prompt: String): ChatResult {
        return chatHelper.run(modelId, prompt)
    }

    suspend fun runChatStreaming(
        modelId: String,
        prompt: String,
        onToken: (String) -> Unit,
        onStats: (ChatPerfStats) -> Unit
    ): ChatResult {
        return chatHelper.runStreaming(modelId, prompt, onToken, onStats)
    }

    suspend fun synthesize(modelId: String, text: String): TtsResult {
        return ttsHelper.synthesize(modelId, text)
    }

    suspend fun transcribe(modelId: String, audioFile: File): Result<String> {
        return asrHelper.recognize(modelId, audioFile)
    }
}
