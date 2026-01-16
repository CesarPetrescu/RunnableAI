package ai.runnable.local

import ai.runnable.local.data.ModelStatus
import ai.runnable.local.data.ModelArtifact
import ai.runnable.local.data.ModelRecord
import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RuntimeType
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
    private val settings = container.settings
    private val huggingFace = container.huggingFace

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    private val _chat = MutableStateFlow(ChatState())
    val chat: StateFlow<ChatState> = _chat.asStateFlow()

    private val _voice = MutableStateFlow(VoiceState())
    val voice: StateFlow<VoiceState> = _voice.asStateFlow()

    private val _hfToken = MutableStateFlow(settings.huggingFaceToken.orEmpty())
    val hfToken: StateFlow<String> = _hfToken.asStateFlow()

    private val _hfSearch = MutableStateFlow(HfSearchState())
    val hfSearch: StateFlow<HfSearchState> = _hfSearch.asStateFlow()

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

    fun updateHfToken(token: String) {
        settings.huggingFaceToken = token
        _hfToken.value = token
    }

    fun updateHfQuery(query: String) {
        _hfSearch.value = _hfSearch.value.copy(query = query)
    }

    fun searchHfGguf() {
        val query = _hfSearch.value.query.trim()
        if (query.isBlank()) return
        _hfSearch.value = _hfSearch.value.copy(isSearching = true, error = null, results = emptyList())
        viewModelScope.launch {
            runCatching {
                huggingFace.searchGgufModels(query, limit = 12, token = settings.huggingFaceToken)
            }.onSuccess { results ->
                val ui = results.map {
                    HfModelUi(
                        repoId = it.modelId ?: it.id,
                        downloads = it.downloads,
                        likes = it.likes
                    )
                }
                _hfSearch.value = _hfSearch.value.copy(isSearching = false, results = ui)
            }.onFailure { error ->
                _hfSearch.value = _hfSearch.value.copy(
                    isSearching = false,
                    error = error.message ?: "Hugging Face search failed"
                )
            }
        }
    }

    fun loadHfFiles(repoId: String) {
        val current = _hfSearch.value
        val updated = current.results.map { result ->
            if (result.repoId == repoId) result.copy(filesLoading = true, filesError = null) else result
        }
        _hfSearch.value = current.copy(results = updated)
        viewModelScope.launch {
            runCatching {
                huggingFace.listGgufFiles(repoId, token = settings.huggingFaceToken)
            }.onSuccess { files ->
                val next = _hfSearch.value.results.map { result ->
                    if (result.repoId == repoId) {
                        result.copy(files = files, filesLoading = false, filesError = null)
                    } else {
                        result
                    }
                }
                _hfSearch.value = _hfSearch.value.copy(results = next)
            }.onFailure { error ->
                val next = _hfSearch.value.results.map { result ->
                    if (result.repoId == repoId) {
                        result.copy(filesLoading = false, filesError = error.message)
                    } else {
                        result
                    }
                }
                _hfSearch.value = _hfSearch.value.copy(results = next)
            }
        }
    }

    fun downloadHfGguf(repoId: String, filename: String) {
        val modelId = buildHfModelId(repoId, filename)
        val url = huggingFace.buildResolveUrl(repoId, filename)
        val model = ModelRecord(
            id = modelId,
            name = "$repoId • $filename",
            task = ModelTask.CHAT,
            runtime = RuntimeType.LLAMA_CPP,
            artifacts = listOf(ModelArtifact(name = filename, url = url)),
            notes = listOf("Added from Hugging Face")
        )
        container.models.addCustomModelAndDownload(model)
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

    private fun buildHfModelId(repoId: String, filename: String): String {
        val raw = "$repoId-$filename".lowercase()
        val sanitized = raw.replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return "hf-$sanitized"
    }
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

data class HfSearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<HfModelUi> = emptyList(),
    val error: String? = null
)

data class HfModelUi(
    val repoId: String,
    val downloads: Long? = null,
    val likes: Int? = null,
    val files: List<String> = emptyList(),
    val filesLoading: Boolean = false,
    val filesError: String? = null
)
