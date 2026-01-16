package ai.runnable.local.domain

import ai.runnable.local.domain.helpers.AsrHelper
import ai.runnable.local.domain.helpers.ChatHelper
import ai.runnable.local.domain.helpers.TtsHelper

class InferenceOrchestrator(
    private val chatHelper: ChatHelper,
    private val ttsHelper: TtsHelper,
    private val asrHelper: AsrHelper
) {
    suspend fun runChat(modelId: String, prompt: String): ChatResult {
        return chatHelper.run(modelId, prompt)
    }

    suspend fun synthesize(modelId: String, text: String): TtsResult {
        return ttsHelper.synthesize(modelId, text)
    }
}
