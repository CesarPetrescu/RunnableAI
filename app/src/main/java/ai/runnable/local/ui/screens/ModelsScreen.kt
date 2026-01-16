package ai.runnable.local.ui.screens

import ai.runnable.local.HfSearchState
import ai.runnable.local.MainViewModel
import ai.runnable.local.data.ModelRecord
import ai.runnable.local.data.ModelStatus
import ai.runnable.local.ui.components.HeroPanel
import ai.runnable.local.ui.components.InfoBanner
import ai.runnable.local.ui.components.ModelBadge
import ai.runnable.local.ui.components.SectionHeader
import ai.runnable.local.ui.components.StatPill
import ai.runnable.local.ui.components.WindowWidthClass
import ai.runnable.local.ui.components.formatBytes
import ai.runnable.local.ui.components.rememberWindowInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ModelsScreen(viewModel: MainViewModel) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val catalogError by viewModel.catalogError.collectAsStateWithLifecycle()
    val hfToken by viewModel.hfToken.collectAsStateWithLifecycle()
    val hfSearch by viewModel.hfSearch.collectAsStateWithLifecycle()

    val total = catalog.size
    val ready = statuses.values.count { it is ModelStatus.Ready }
    val downloading = statuses.values.count { it is ModelStatus.Downloading }
    val totalBytes = catalog.sumOf { record -> record.artifacts.sumOf { it.bytes } }

    val windowInfo = rememberWindowInfo()
    val isWide = windowInfo.widthClass == WindowWidthClass.Expanded

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDetail by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(catalog) {
        if (selectedId == null && catalog.isNotEmpty()) {
            selectedId = catalog.first().id
        }
    }

    val selectedModel = catalog.firstOrNull { it.id == selectedId }
    val dependencyNames = selectedModel?.dependsOn?.map { depId ->
        catalog.firstOrNull { it.id == depId }?.name ?: depId
    }.orEmpty()
    val selectedFiles = selectedModel?.let { viewModel.modelFiles(it.id) }.orEmpty()
    val selectedStatus = selectedModel?.let { statuses[it.id] } ?: ModelStatus.NotDownloaded

    if (isWide) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModelsList(
                total = total,
                ready = ready,
                downloading = downloading,
                totalBytes = totalBytes,
                catalog = catalog,
                statuses = statuses,
                catalogError = catalogError,
                onRefresh = { viewModel.refreshCatalog() },
                onDownload = { viewModel.downloadModel(it) },
                onRemove = { viewModel.removeModel(it) },
                hfToken = hfToken,
                hfSearch = hfSearch,
                onHfTokenChange = { viewModel.updateHfToken(it) },
                onHfQueryChange = { viewModel.updateHfQuery(it) },
                onHfSearch = { viewModel.searchHfGguf() },
                onHfLoadFiles = { viewModel.loadHfFiles(it) },
                onHfDownload = { repoId, filename -> viewModel.downloadHfGguf(repoId, filename) },
                onSelect = {
                    selectedId = it
                    showDetail = true
                },
                selectedId = selectedId,
                modifier = Modifier.weight(0.55f)
            )

            Column(modifier = Modifier.weight(0.45f)) {
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedModel != null) {
                    ModelDetailScreen(
                        model = selectedModel,
                        status = selectedStatus,
                        files = selectedFiles,
                        dependencyNames = dependencyNames,
                        onDownload = { viewModel.downloadModel(selectedModel.id) },
                        onRemove = { viewModel.removeModel(selectedModel.id) }
                    )
                } else {
                    EmptyDetailState()
                }
            }
        }
    } else {
        BackHandler(enabled = showDetail) {
            showDetail = false
        }
        if (showDetail && selectedModel != null) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ModelDetailScreen(
                        model = selectedModel,
                        status = selectedStatus,
                        files = selectedFiles,
                        dependencyNames = dependencyNames,
                        onDownload = { viewModel.downloadModel(selectedModel.id) },
                        onRemove = { viewModel.removeModel(selectedModel.id) },
                        onBack = { showDetail = false }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        } else {
            ModelsList(
                total = total,
                ready = ready,
                downloading = downloading,
                totalBytes = totalBytes,
                catalog = catalog,
                statuses = statuses,
                catalogError = catalogError,
                onRefresh = { viewModel.refreshCatalog() },
                onDownload = { viewModel.downloadModel(it) },
                onRemove = { viewModel.removeModel(it) },
                hfToken = hfToken,
                hfSearch = hfSearch,
                onHfTokenChange = { viewModel.updateHfToken(it) },
                onHfQueryChange = { viewModel.updateHfQuery(it) },
                onHfSearch = { viewModel.searchHfGguf() },
                onHfLoadFiles = { viewModel.loadHfFiles(it) },
                onHfDownload = { repoId, filename -> viewModel.downloadHfGguf(repoId, filename) },
                onSelect = {
                    selectedId = it
                    showDetail = true
                },
                selectedId = selectedId,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ModelsList(
    total: Int,
    ready: Int,
    downloading: Int,
    totalBytes: Long,
    catalog: List<ModelRecord>,
    statuses: Map<String, ModelStatus>,
    catalogError: String?,
    onRefresh: () -> Unit,
    onDownload: (String) -> Unit,
    onRemove: (String) -> Unit,
    hfToken: String,
    hfSearch: HfSearchState,
    onHfTokenChange: (String) -> Unit,
    onHfQueryChange: (String) -> Unit,
    onHfSearch: () -> Unit,
    onHfLoadFiles: (String) -> Unit,
    onHfDownload: (String, String) -> Unit,
    onSelect: (String) -> Unit,
    selectedId: String?,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeroPanel(
                title = "Model Vault",
                subtitle = "Download GGUF, ONNX, and ExecuTorch assets once, run locally forever."
            )
        }

        item {
            SectionHeader(
                title = "Library",
                subtitle = "Track storage and active downloads at a glance."
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill(value = total.toString(), label = "Models")
                StatPill(value = ready.toString(), label = "Ready")
                StatPill(value = downloading.toString(), label = "Downloading")
                StatPill(value = formatBytes(totalBytes), label = "Catalog")
            }
        }

        if (catalogError != null) {
            item {
                InfoBanner(
                    title = catalogError,
                    actionLabel = "Retry",
                    onAction = onRefresh
                )
            }
        }

        item {
            SectionHeader(
                title = "Hugging Face GGUF",
                subtitle = "Search repos, then download GGUF files with your token."
            )
        }

        item {
            HuggingFacePanel(
                token = hfToken,
                search = hfSearch,
                onTokenChange = onHfTokenChange,
                onQueryChange = onHfQueryChange,
                onSearch = onHfSearch,
                onLoadFiles = onHfLoadFiles,
                onDownload = onHfDownload
            )
        }

        item {
            SectionHeader(
                title = "Available models",
                subtitle = "Tap a card for details."
            )
        }

        items(catalog, key = { it.id }) { model ->
            val status = statuses[model.id] ?: ModelStatus.NotDownloaded
            ModelCard(
                model = model,
                status = status,
                onDownload = { onDownload(model.id) },
                onRemove = { onRemove(model.id) },
                onSelect = { onSelect(model.id) },
                isSelected = model.id == selectedId
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelRecord,
    status: ModelStatus,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onSelect: () -> Unit,
    isSelected: Boolean
) {
    val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = border,
        modifier = Modifier.clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            ModelBadge(task = model.task, runtime = model.runtime)

            if (model.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = model.notes.joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            model.requirements?.minRamMb?.let { minRam ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Min RAM: ${minRam}MB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (model.dependsOn.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Depends on: ${model.dependsOn.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            when (status) {
                is ModelStatus.NotDownloaded -> {
                    Button(onClick = onDownload) {
                        Text("Download")
                    }
                }
                is ModelStatus.Downloading -> {
                    Column {
                        DownloadProgress(status.progress)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                is ModelStatus.Ready -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ready",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = onRemove) {
                            Text("Remove")
                        }
                    }
                }
                is ModelStatus.Failed -> {
                    Column {
                        Text(
                            text = "Failed: ${status.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(onClick = onDownload) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: Float) {
    val clamped = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun HuggingFacePanel(
    token: String,
    search: HfSearchState,
    onTokenChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadFiles: (String) -> Unit,
    onDownload: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            label = { Text("Hugging Face token") },
            placeholder = { Text("hf_...") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = search.query,
            onValueChange = onQueryChange,
            label = { Text("Search GGUF repos") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSearch, enabled = !search.isSearching) {
                Text(if (search.isSearching) "Searching..." else "Search")
            }
            if (search.error != null) {
                Text(
                    text = search.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        if (search.results.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                search.results.forEach { result ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = result.repoId,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (result.downloads != null || result.likes != null) {
                                val meta = buildString {
                                    result.downloads?.let { append("Downloads: $it") }
                                    if (isNotEmpty() && result.likes != null) append(" • ")
                                    result.likes?.let { append("Likes: $it") }
                                }
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            when {
                                result.filesLoading -> Text("Loading GGUF files…")
                                result.filesError != null -> {
                                    Text(
                                        text = result.filesError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    OutlinedButton(onClick = { onLoadFiles(result.repoId) }) {
                                        Text("Retry files")
                                    }
                                }
                                result.files.isEmpty() -> {
                                    OutlinedButton(onClick = { onLoadFiles(result.repoId) }) {
                                        Text("Load GGUF files")
                                    }
                                }
                                else -> {
                                    result.files.forEach { file ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = file,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(onClick = { onDownload(result.repoId, file) }) {
                                                Text("Download")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDetailState() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Select a model",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Details appear here on larger screens.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
