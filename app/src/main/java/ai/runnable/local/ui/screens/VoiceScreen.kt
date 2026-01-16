package ai.runnable.local.ui.screens

import ai.runnable.local.MainViewModel
import ai.runnable.local.data.ModelTask
import ai.runnable.local.ui.components.HeroPanel
import ai.runnable.local.ui.components.ModelPicker
import ai.runnable.local.ui.components.SectionHeader
import ai.runnable.local.ui.components.WindowWidthClass
import ai.runnable.local.ui.components.rememberWindowInfo
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VoiceScreen(viewModel: MainViewModel) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val voice by viewModel.voice.collectAsStateWithLifecycle()

    val ttsModels = catalog.filter { it.task == ModelTask.TTS }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var text by rememberSaveable { mutableStateOf("Welcome to RunnableAI — your local voice studio.") }

    LaunchedEffect(ttsModels) {
        if (selectedId == null && ttsModels.isNotEmpty()) {
            selectedId = ttsModels.first().id
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
                    subtitle = "Generate speech locally with ExecuTorch or GGUF backbones."
                )
                Spacer(modifier = Modifier.height(16.dp))
                VoiceModelCard(
                    models = ttsModels,
                    statuses = statuses,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                VoicePromptCard(
                    text = text,
                    onTextChange = { text = it },
                    styles = styles,
                    onSpeak = {
                        val id = selectedId ?: return@VoicePromptCard
                        viewModel.synthesize(id, text)
                    },
                    isRunning = voice.isRunning,
                    canRun = selectedId != null
                )
            }
            Column(modifier = Modifier.weight(0.45f)) {
                Spacer(modifier = Modifier.height(8.dp))
                PlaybackCard(
                    lastSampleCount = voice.lastSampleCount,
                    error = voice.error
                )
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
                    subtitle = "Generate speech locally with ExecuTorch or GGUF backbones."
                )
            }

            item {
                VoiceModelCard(
                    models = ttsModels,
                    statuses = statuses,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
            }

            item {
                VoicePromptCard(
                    text = text,
                    onTextChange = { text = it },
                    styles = styles,
                    onSpeak = {
                        val id = selectedId ?: return@VoicePromptCard
                        viewModel.synthesize(id, text)
                    },
                    isRunning = voice.isRunning,
                    canRun = selectedId != null
                )
            }

            item {
                PlaybackCard(
                    lastSampleCount = voice.lastSampleCount,
                    error = voice.error
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun VoiceModelCard(
    models: List<ai.runnable.local.data.ModelRecord>,
    statuses: Map<String, ai.runnable.local.data.ModelStatus>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    SectionHeader(
        title = "TTS model",
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
private fun VoicePromptCard(
    text: String,
    onTextChange: (String) -> Unit,
    styles: List<String>,
    onSpeak: () -> Unit,
    isRunning: Boolean,
    canRun: Boolean
) {
    SectionHeader(
        title = "Voice prompt",
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSpeak,
                    enabled = !isRunning && canRun
                ) {
                    Text(if (isRunning) "Speaking..." else "Speak")
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
                lastSampleCount > 0 -> "Played ${lastSampleCount} samples"
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
private fun StyleChip(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
