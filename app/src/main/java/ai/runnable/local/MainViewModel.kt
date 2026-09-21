package ai.runnable.local

import ai.runnable.local.data.ModelStatus
import ai.runnable.local.data.ModelArtifact
import ai.runnable.local.data.ModelRecord
import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RecordingEntry
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
    private val recordingStore = container.recordingStore
    private val audioRecorder = container.audioRecorder
    private val recordingPlayer = container.recordingPlayer

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    private val _chat = MutableStateFlow(ChatState())
    val chat: StateFlow<ChatState> = _chat.asStateFlow()

    private val _voice = MutableStateFlow(VoiceState())
    val voice: StateFlow<VoiceState> = _voice.asStateFlow()

    private val _recordings = MutableStateFlow(loadRecordings())
    val recordings: StateFlow<List<RecordingUi>> = _recordings.asStateFlow()

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

    fun cancelDownload(modelId: String) {
        container.models.cancelDownload(modelId)
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
        _hfSearch.value = _hfSearch.value.copy(
            isSearching = true,
            isLoadingMore = false,
            error = null,
            results = emptyList(),
            nextCursor = null
        )
        viewModelScope.launch {
            runCatching {
                huggingFace.searchGgufModels(
                    query = query,
                    limit = 20,
                    token = settings.huggingFaceToken
                )
            }.onSuccess { response ->
                val ui = response.models.map {
                    HfModelUi(
                        repoId = it.modelId ?: it.id,
                        downloads = it.downloads,
                        likes = it.likes
                    )
                }
                _hfSearch.value = _hfSearch.value.copy(
                    isSearching = false,
                    results = ui,
                    nextCursor = response.nextCursor
                )
            }.onFailure { error ->
                _hfSearch.value = _hfSearch.value.copy(
                    isSearching = false,
                    error = error.message ?: "Hugging Face search failed"
                )
            }
        }
    }

    fun loadMoreHfGguf() {
        val state = _hfSearch.value
        val query = state.query.trim()
        val cursor = state.nextCursor ?: return
        if (query.isBlank() || state.isLoadingMore || state.isSearching) return
        _hfSearch.value = state.copy(isLoadingMore = true, error = null)
        viewModelScope.launch {
            runCatching {
                huggingFace.searchGgufModels(
                    query = query,
                    limit = 20,
                    token = settings.huggingFaceToken,
                    cursor = cursor
                )
            }.onSuccess { response ->
                val next = response.models.map {
                    HfModelUi(
                        repoId = it.modelId ?: it.id,
                        downloads = it.downloads,
                        likes = it.likes
                    )
                }
                val merged = (state.results + next).distinctBy { it.repoId }
                _hfSearch.value = _hfSearch.value.copy(
                    isLoadingMore = false,
                    results = merged,
                    nextCursor = response.nextCursor
                )
            }.onFailure { error ->
                _hfSearch.value = _hfSearch.value.copy(
                    isLoadingMore = false,
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
            }.onSuccess { listing ->
                val next = _hfSearch.value.results.map { result ->
                    if (result.repoId == repoId) {
                        result.copy(
                            files = listing.files,
                            revision = listing.revision,
                            gated = listing.gated,
                            isPrivate = listing.isPrivate,
                            filesLoading = false,
                            filesError = null
                        )
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
        val revision = _hfSearch.value.results.firstOrNull { it.repoId == repoId }?.revision ?: "main"
        val modelId = buildHfModelId(repoId, filename)
        val url = huggingFace.buildResolveUrl(repoId, filename, revision)
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
            val withAssistant = withUser + ChatMessage(ChatRole.ASSISTANT, "")
            _chat.value = _chat.value.copy(
                isRunning = true,
                error = null,
                output = "",
                streamText = "",
                perf = null,
                messages = withAssistant
            )
            when (
                val result = container.orchestrator.runChatStreaming(
                    modelId,
                    prompt,
                    onToken = { token ->
                        viewModelScope.launch {
                            _chat.value = _chat.value.copy(
                                streamText = _chat.value.streamText + token,
                                messages = appendAssistantToken(_chat.value.messages, token)
                            )
                        }
                    },
                    onStats = { stats ->
                        viewModelScope.launch {
                            _chat.value = _chat.value.copy(
                                perf = ChatPerf(
                                    promptTokens = stats.promptTokens,
                                    promptMs = stats.promptMs,
                                    promptTokensPerSec = stats.promptTokensPerSec,
                                    genTokens = stats.genTokens,
                                    genMs = stats.genMs,
                                    genTokensPerSec = stats.genTokensPerSec
                                )
                            )
                        }
                    }
                )
            ) {
                is ChatResult.Success -> {
                    val finalText = _chat.value.streamText.ifBlank { result.text }
                    val updated = replaceAssistantMessage(_chat.value.messages, finalText)
                    _chat.value = _chat.value.copy(
                        isRunning = false,
                        output = finalText,
                        streamText = finalText,
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

    fun startRecording() {
        if (_voice.value.isRecording) return
        val outputFile = recordingStore.createRecordingFile()
        runCatching {
            audioRecorder.start(outputFile)
        }.onSuccess {
            _voice.value = _voice.value.copy(isRecording = true, recordingError = null)
        }.onFailure { error ->
            _voice.value = _voice.value.copy(recordingError = error.message ?: "Failed to start recording")
        }
    }

    fun stopRecording() {
        if (!_voice.value.isRecording) return
        viewModelScope.launch {
            runCatching { audioRecorder.stop() }
                .onSuccess { result ->
                    val entry = RecordingEntry(
                        id = result.file.nameWithoutExtension.removePrefix("rec-"),
                        fileName = result.file.name,
                        createdAt = System.currentTimeMillis(),
                        durationMs = result.durationMs
                    )
                    recordingStore.add(entry)
                    _recordings.value = loadRecordings()
                    _voice.value = _voice.value.copy(isRecording = false, recordingError = null)
                }
                .onFailure { error ->
                    _voice.value = _voice.value.copy(
                        isRecording = false,
                        recordingError = error.message ?: "Failed to stop recording"
                    )
                }
        }
    }

    fun playRecording(id: String) {
        val entry = recordingStore.list().firstOrNull { it.id == id } ?: return
        val file = recordingStore.fileFor(entry)
        _voice.value = _voice.value.copy(playingId = id)
        recordingPlayer.play(file) {
            _voice.value = _voice.value.copy(playingId = null)
        }
    }

    fun transcribeRecording(id: String, modelId: String) {
        val entry = recordingStore.list().firstOrNull { it.id == id } ?: return
        val file = recordingStore.fileFor(entry)
        _voice.value = _voice.value.copy(transcribingId = id, recordingError = null)
        viewModelScope.launch {
            val result = container.orchestrator.transcribe(modelId, file)
            if (result.isSuccess) {
                val transcript = result.getOrNull().orEmpty()
                recordingStore.updateTranscript(id, transcript)
                _recordings.value = loadRecordings()
                _voice.value = _voice.value.copy(transcribingId = null)
            } else {
                val message = result.exceptionOrNull()?.message ?: "Transcription failed"
                recordingStore.updateTranscript(id, message)
                _recordings.value = loadRecordings()
                _voice.value = _voice.value.copy(transcribingId = null, recordingError = message)
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

    private fun appendAssistantToken(messages: List<ChatMessage>, token: String): List<ChatMessage> {
        val index = messages.indexOfLast { it.role == ChatRole.ASSISTANT }
        if (index == -1) return messages
        val updated = messages.toMutableList()
        val current = updated[index]
        updated[index] = current.copy(text = current.text + token)
        return updated
    }

    private fun replaceAssistantMessage(messages: List<ChatMessage>, text: String): List<ChatMessage> {
        val index = messages.indexOfLast { it.role == ChatRole.ASSISTANT }
        if (index == -1) return messages + ChatMessage(ChatRole.ASSISTANT, text)
        val updated = messages.toMutableList()
        updated[index] = updated[index].copy(text = text)
        return updated
    }

    private fun loadRecordings(): List<RecordingUi> {
        return recordingStore.list().map { entry ->
            RecordingUi(
                id = entry.id,
                fileName = entry.fileName,
                createdAt = entry.createdAt,
                durationMs = entry.durationMs,
                transcript = entry.transcript
            )
        }
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
    val messages: List<ChatMessage> = emptyList(),
    val streamText: String = "",
    val perf: ChatPerf? = null
)

data class VoiceState(
    val isRunning: Boolean = false,
    val lastSampleCount: Int = 0,
    val error: String? = null,
    val isRecording: Boolean = false,
    val recordingError: String? = null,
    val playingId: String? = null,
    val transcribingId: String? = null
)

data class ChatPerf(
    val promptTokens: Int,
    val promptMs: Long,
    val promptTokensPerSec: Float,
    val genTokens: Int,
    val genMs: Long,
    val genTokensPerSec: Float
)

data class RecordingUi(
    val id: String,
    val fileName: String,
    val createdAt: Long,
    val durationMs: Long,
    val transcript: String
)

data class HfSearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val results: List<HfModelUi> = emptyList(),
    val error: String? = null,
    val nextCursor: String? = null
)

data class HfModelUi(
    val repoId: String,
    val downloads: Long? = null,
    val likes: Int? = null,
    val files: List<String> = emptyList(),
    val revision: String = "main",
    val gated: Boolean = false,
    val isPrivate: Boolean = false,
    val filesLoading: Boolean = false,
    val filesError: String? = null
)
