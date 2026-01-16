package ai.runnable.local

import ai.runnable.local.data.ModelStatus
import ai.runnable.local.domain.ChatMessage
import ai.runnable.local.domain.ChatResult
import ai.runnable.local.domain.ChatRole
import ai.runnable.local.domain.TtsResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {
    val catalog = container.models.catalog
    val statuses = container.models.statuses

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    private val _chat = MutableStateFlow(ChatState())
    val chat: StateFlow<ChatState> = _chat.asStateFlow()

    private val _voice = MutableStateFlow(VoiceState())
    val voice: StateFlow<VoiceState> = _voice.asStateFlow()

    init {
        refreshCatalog()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            runCatching {
                container.models.refreshCatalog()
            }.onFailure { error ->
                _catalogError.value = error.message ?: "Failed to load catalog"
            }.onSuccess {
                _catalogError.value = null
            }
        }
    }

    fun downloadModel(modelId: String) {
        container.models.downloadModel(modelId)
    }

    fun removeModel(modelId: String) {
        container.models.removeModel(modelId)
    }

    fun runChat(modelId: String, prompt: String) {
        viewModelScope.launch {
            val withUser = _chat.value.messages + ChatMessage(ChatRole.USER, prompt)
            _chat.value = _chat.value.copy(isRunning = true, error = null, messages = withUser)
            when (val result = container.orchestrator.runChat(modelId, prompt)) {
                is ChatResult.Success -> {
                    val updated = _chat.value.messages + ChatMessage(ChatRole.ASSISTANT, result.text)
                    _chat.value = _chat.value.copy(
                        isRunning = false,
                        output = result.text,
                        messages = updated
                    )
                }
                is ChatResult.Error -> {
                    val updated = _chat.value.messages + ChatMessage(ChatRole.SYSTEM, result.message)
                    _chat.value = _chat.value.copy(
                        isRunning = false,
                        error = result.message,
                        messages = updated
                    )
                }
            }
        }
    }

    fun synthesize(modelId: String, text: String) {
        viewModelScope.launch {
            _voice.value = _voice.value.copy(isRunning = true, error = null)
            when (val result = container.orchestrator.synthesize(modelId, text)) {
                is TtsResult.Success -> {
                    _voice.value = _voice.value.copy(isRunning = false, lastSampleCount = result.samples)
                }
                is TtsResult.Error -> {
                    _voice.value = _voice.value.copy(isRunning = false, error = result.message)
                }
            }
        }
    }

    fun downloadStatus(modelId: String): ModelStatus {
        return statuses.value[modelId] ?: ModelStatus.NotDownloaded
    }

    fun clearChat() {
        _chat.value = ChatState()
    }

    fun modelFiles(modelId: String) = container.models.modelFiles(modelId)
}

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class ChatState(
    val isRunning: Boolean = false,
    val output: String = "",
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

data class VoiceState(
    val isRunning: Boolean = false,
    val lastSampleCount: Int = 0,
    val error: String? = null
)
