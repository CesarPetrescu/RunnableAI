package ai.runnable.local.domain

sealed class ChatResult {
    data class Success(val text: String) : ChatResult()
    data class Error(val message: String) : ChatResult()
}

sealed class TtsResult {
    data class Success(val samples: Int) : TtsResult()
    data class Error(val message: String) : TtsResult()
}
