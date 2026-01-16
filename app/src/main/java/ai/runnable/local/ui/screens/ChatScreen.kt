package ai.runnable.local.ui.screens

import ai.runnable.local.MainViewModel
import ai.runnable.local.data.ModelTask
import ai.runnable.local.domain.ChatRole
import ai.runnable.local.ui.components.ChatActions
import ai.runnable.local.ui.components.ChatHistoryList
import ai.runnable.local.ui.components.HeroPanel
import ai.runnable.local.ui.components.ModelPicker
import ai.runnable.local.ui.components.SectionHeader
import ai.runnable.local.ui.components.TokenStreamBar
import ai.runnable.local.ui.components.WindowWidthClass
import ai.runnable.local.ui.components.rememberWindowInfo
import ai.runnable.local.ui.theme.RunnableTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
fun ChatScreen(viewModel: MainViewModel) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val chat by viewModel.chat.collectAsStateWithLifecycle()

    val llmModels = catalog.filter { it.task == ModelTask.CHAT }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var prompt by rememberSaveable { mutableStateOf("Give me a concise plan for a local AI app.") }

    LaunchedEffect(llmModels) {
        if (selectedId == null && llmModels.isNotEmpty()) {
            selectedId = llmModels.first().id
        }
    }

    val quickPrompts = listOf(
        "Summarize this idea in 5 bullets.",
        "Draft a launch checklist for Android MVP.",
        "Explain model download flow for users.",
        "Write a short onboarding message."
    )

    val lastAssistant = chat.messages.lastOrNull { it.role == ChatRole.ASSISTANT }?.text.orEmpty()
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
                    title = "Prompt Studio",
                    subtitle = "Craft and test prompts against your local GGUF chat model."
                )
                Spacer(modifier = Modifier.height(16.dp))
                ChatModelCard(
                    llmModels = llmModels,
                    statuses = statuses,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PromptCard(
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    quickPrompts = quickPrompts,
                    onPromptSelect = { prompt = it },
                    onRun = {
                        val id = selectedId ?: return@PromptCard
                        viewModel.runChat(id, prompt)
                    },
                    isRunning = chat.isRunning,
                    canRun = selectedId != null
                )
            }
            Column(modifier = Modifier.weight(0.45f)) {
                Spacer(modifier = Modifier.height(8.dp))
                OutputCard(
                    messages = chat.messages,
                    error = chat.error,
                    tokenStream = lastAssistant,
                    maxHistoryHeight = 420.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                ChatActions(
                    textToShare = lastAssistant,
                    onClear = { viewModel.clearChat() }
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
                    title = "Prompt Studio",
                    subtitle = "Craft and test prompts against your local GGUF chat model."
                )
            }

            item {
                ChatModelCard(
                    llmModels = llmModels,
                    statuses = statuses,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
            }

            item {
                PromptCard(
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    quickPrompts = quickPrompts,
                    onPromptSelect = { prompt = it },
                    onRun = {
                        val id = selectedId ?: return@PromptCard
                        viewModel.runChat(id, prompt)
                    },
                    isRunning = chat.isRunning,
                    canRun = selectedId != null
                )
            }

            item {
                OutputCard(
                    messages = chat.messages,
                    error = chat.error,
                    tokenStream = lastAssistant,
                    maxHistoryHeight = 320.dp
                )
            }

            item {
                ChatActions(
                    textToShare = lastAssistant,
                    onClear = { viewModel.clearChat() }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ChatModelCard(
    llmModels: List<ai.runnable.local.data.ModelRecord>,
    statuses: Map<String, ai.runnable.local.data.ModelStatus>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    SectionHeader(
        title = "Chat model",
        subtitle = "Choose a local model and run on-device."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            ModelPicker(
                label = "Select a chat model",
                models = llmModels,
                statuses = statuses,
                selectedId = selectedId,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PromptCard(
    prompt: String,
    onPromptChange: (String) -> Unit,
    quickPrompts: List<String>,
    onPromptSelect: (String) -> Unit,
    onRun: () -> Unit,
    isRunning: Boolean,
    canRun: Boolean
) {
    SectionHeader(
        title = "Prompt",
        subtitle = "Keep it short for fast response."
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quickPrompts) { suggestion ->
                    QuickPromptChip(
                        text = suggestion,
                        onClick = { onPromptSelect(suggestion) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRun,
                enabled = !isRunning && canRun
            ) {
                Text(if (isRunning) "Running..." else "Run")
            }
        }
    }
}

@Composable
private fun OutputCard(
    messages: List<ai.runnable.local.domain.ChatMessage>,
    error: String?,
    tokenStream: String,
    maxHistoryHeight: androidx.compose.ui.unit.Dp
) {
    SectionHeader(
        title = "Session",
        subtitle = "Conversation history and token stream."
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            ChatHistoryList(
                messages = messages,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = maxHistoryHeight)
            )
            if (tokenStream.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Token stream",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                TokenStreamBar(text = tokenStream)
            }
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun QuickPromptChip(text: String, onClick: () -> Unit) {
    val colors = RunnableTheme.colors
    Surface(
        onClick = onClick,
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

