package ai.runnable.local.ui.screens

import ai.runnable.local.MainViewModel
import ai.runnable.local.data.ModelTask
import ai.runnable.local.ui.components.HeroPanel
import ai.runnable.local.ui.components.ModelPicker
import ai.runnable.local.ui.components.SectionHeader
import ai.runnable.local.ui.components.WindowWidthClass
import ai.runnable.local.ui.components.rememberWindowInfo
import ai.runnable.local.ui.theme.RunnableTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VoiceScreen(viewModel: MainViewModel) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val voice by viewModel.voice.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()

    val ttsModels = catalog.filter { it.task == ModelTask.TTS }
    val asrModels = catalog.filter { it.task == ModelTask.ASR }

    var selectedTtsId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAsrId by rememberSaveable { mutableStateOf<String?>(null) }
    var ttsText by rememberSaveable { mutableStateOf("Welcome to RunnableAI — your local voice studio.") }
    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0 = TTS, 1 = STT
    val context = LocalContext.current
    var hasMicPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(ttsModels) {
        if (selectedTtsId == null && ttsModels.isNotEmpty()) {
            selectedTtsId = ttsModels.first().id
        }
    }

    LaunchedEffect(asrModels) {
        if (selectedAsrId == null && asrModels.isNotEmpty()) {
            selectedAsrId = asrModels.first().id
        }
    }

    val styles = listOf("Neutral", "Bright", "Calm", "Broadcast", "Whisper")
    val windowInfo = rememberWindowInfo()

    if (windowInfo.widthClass == WindowWidthClass.Expanded) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(0.55f)) {
                Spacer(modifier = Modifier.height(8.dp))
                HeroPanel(
                    title = "Voice Lab",
                    subtitle = "Text-to-Speech and Speech-to-Text on-device."
                )
                Spacer(modifier = Modifier.height(16.dp))
                VoiceTabSelector(
                    selectedTab = activeTab,
                    onTabSelect = { activeTab = it }
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    // TTS Section
                    TtsModelCard(
                        models = ttsModels,
                        statuses = statuses,
                        selectedId = selectedTtsId,
                        onSelect = { selectedTtsId = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TtsPromptCard(
                        text = ttsText,
                        onTextChange = { ttsText = it },
                        styles = styles,
                        onSpeak = {
                            val id = selectedTtsId ?: return@TtsPromptCard
                            viewModel.synthesize(id, ttsText)
                        },
                        isRunning = voice.isRunning,
                        canRun = selectedTtsId != null
                    )
                } else {
                    // STT Section
                    SttModelCard(
                        models = asrModels,
                        statuses = statuses,
                        selectedId = selectedAsrId,
                        onSelect = { selectedAsrId = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SttRecordCard(
                        isRecording = voice.isRecording,
                        hasMicPermission = hasMicPermission,
                        error = voice.recordingError,
                        onStartRecording = {
                            if (hasMicPermission) {
                                viewModel.startRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopRecording = { viewModel.stopRecording() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RecordingsCard(
                        recordings = recordings,
                        playingId = voice.playingId,
                        transcribingId = voice.transcribingId,
                        canTranscribe = selectedAsrId != null,
                        onPlay = { viewModel.playRecording(it) },
                        onTranscribe = {
                            val modelId = selectedAsrId ?: return@RecordingsCard
                            viewModel.transcribeRecording(it, modelId)
                        }
                    )
                }
            }
            Column(modifier = Modifier.weight(0.45f)) {
                Spacer(modifier = Modifier.height(8.dp))
                if (activeTab == 0) {
                    PlaybackCard(
                        lastSampleCount = voice.lastSampleCount,
                        error = voice.error
                    )
                } else {
                    TranscriptCard(
                        transcript = "",
                        error = null
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeroPanel(
                    title = "Voice Lab",
                    subtitle = "Text-to-Speech and Speech-to-Text on-device."
                )
            }

            item {
                VoiceTabSelector(
                    selectedTab = activeTab,
                    onTabSelect = { activeTab = it }
                )
            }

            if (activeTab == 0) {
                // TTS Section
                item {
                    TtsModelCard(
                        models = ttsModels,
                        statuses = statuses,
                        selectedId = selectedTtsId,
                        onSelect = { selectedTtsId = it }
                    )
                }

                item {
                    TtsPromptCard(
                        text = ttsText,
                        onTextChange = { ttsText = it },
                        styles = styles,
                        onSpeak = {
                            val id = selectedTtsId ?: return@TtsPromptCard
                            viewModel.synthesize(id, ttsText)
                        },
                        isRunning = voice.isRunning,
                        canRun = selectedTtsId != null
                    )
                }

                item {
                    PlaybackCard(
                        lastSampleCount = voice.lastSampleCount,
                        error = voice.error
                    )
                }
            } else {
                // STT Section
                item {
                    SttModelCard(
                        models = asrModels,
                        statuses = statuses,
                        selectedId = selectedAsrId,
                        onSelect = { selectedAsrId = it }
                    )
                }

                item {
                    SttRecordCard(
                        isRecording = voice.isRecording,
                        hasMicPermission = hasMicPermission,
                        error = voice.recordingError,
                        onStartRecording = {
                            if (hasMicPermission) {
                                viewModel.startRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopRecording = { viewModel.stopRecording() }
                    )
                }

                item {
                    RecordingsCard(
                        recordings = recordings,
                        playingId = voice.playingId,
                        transcribingId = voice.transcribingId,
                        canTranscribe = selectedAsrId != null,
                        onPlay = { viewModel.playRecording(it) },
                        onTranscribe = {
                            val modelId = selectedAsrId ?: return@RecordingsCard
                            viewModel.transcribeRecording(it, modelId)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun VoiceTabSelector(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val colors = RunnableTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = { onTabSelect(0) },
            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = "Text to Speech",
                style = MaterialTheme.typography.labelLarge,
                color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
        Surface(
            onClick = { onTabSelect(1) },
            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = "Speech to Text",
                style = MaterialTheme.typography.labelLarge,
                color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun TtsModelCard(
    models: List<ai.runnable.local.data.ModelRecord>,
    statuses: Map<String, ai.runnable.local.data.ModelStatus>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    SectionHeader(
        title = "TTS Model",
        subtitle = "Pick a voice model to synthesize speech."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            ModelPicker(
                label = "Select a TTS model",
                models = models,
                statuses = statuses,
                selectedId = selectedId,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SttModelCard(
    models: List<ai.runnable.local.data.ModelRecord>,
    statuses: Map<String, ai.runnable.local.data.ModelStatus>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    SectionHeader(
        title = "STT Model",
        subtitle = "Pick an ASR model for transcription."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (models.isEmpty()) {
                Text(
                    text = "No ASR models in catalog. Add ONNX ASR models to catalog.json.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ModelPicker(
                    label = "Select an ASR model",
                    models = models,
                    statuses = statuses,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TtsPromptCard(
    text: String,
    onTextChange: (String) -> Unit,
    styles: List<String>,
    onSpeak: () -> Unit,
    isRunning: Boolean,
    canRun: Boolean
) {
    SectionHeader(
        title = "Voice Prompt",
        subtitle = "Shorter text yields faster synthesis."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(styles) { style ->
                    StyleChip(text = style)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSpeak,
                enabled = !isRunning && canRun
            ) {
                Text(if (isRunning) "Speaking..." else "Speak")
            }
        }
    }
}

@Composable
private fun SttRecordCard(
    isRecording: Boolean,
    hasMicPermission: Boolean,
    error: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    SectionHeader(
        title = "Record Audio",
        subtitle = "Tap to start recording, transcription runs locally."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (isRecording) {
                    "Recording..."
                } else if (!hasMicPermission) {
                    "Microphone permission is required to record audio."
                } else {
                    "Tap the button to start recording audio for transcription."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isRecording) {
                    Button(onClick = onStopRecording) {
                        Text("Stop Recording")
                    }
                } else {
                    Button(onClick = onStartRecording) {
                        Text("Start Recording")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackCard(
    lastSampleCount: Int,
    error: String?
) {
    SectionHeader(
        title = "Playback",
        subtitle = "Audio output renders here once streaming is wired."
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            val message = error ?: when {
                lastSampleCount > 0 -> "Played $lastSampleCount samples"
                else -> "No audio output yet."
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TranscriptCard(
    transcript: String,
    error: String?
) {
    SectionHeader(
        title = "Transcript",
        subtitle = "Speech recognition output appears here."
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            val message = error ?: transcript.ifBlank { "Record or upload audio to see the transcript." }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RecordingsCard(
    recordings: List<ai.runnable.local.RecordingUi>,
    playingId: String?,
    transcribingId: String?,
    canTranscribe: Boolean,
    onPlay: (String) -> Unit,
    onTranscribe: (String) -> Unit
) {
    SectionHeader(
        title = "Recordings",
        subtitle = "Saved takes with playback and transcription."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (recordings.isEmpty()) {
                Text(
                    text = "No recordings yet. Tap Start Recording to capture audio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recordings.forEach { recording ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = recording.fileName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Duration: ${formatDuration(recording.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (recording.transcript.isNotBlank()) {
                            Text(
                                text = "Transcript: ${recording.transcript}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { onPlay(recording.id) }) {
                                Text(if (playingId == recording.id) "Playing..." else "Play")
                            }
                            Button(
                                onClick = { onTranscribe(recording.id) },
                                enabled = canTranscribe && transcribingId != recording.id
                            ) {
                                Text(
                                    if (!canTranscribe) "Select ASR model"
                                    else if (transcribingId == recording.id) "Transcribing..."
                                    else "Transcribe"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleChip(text: String) {
    val colors = RunnableTheme.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.chipBg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.chipFg,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0s"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
