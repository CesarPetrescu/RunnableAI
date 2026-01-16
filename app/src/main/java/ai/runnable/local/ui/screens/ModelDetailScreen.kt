package ai.runnable.local.ui.screens

import ai.runnable.local.data.ModelRecord
import ai.runnable.local.data.ModelStatus
import ai.runnable.local.ui.components.ArtifactRow
import ai.runnable.local.ui.components.DependencyGraph
import ai.runnable.local.ui.components.ModelBadge
import ai.runnable.local.ui.components.SectionHeader
import ai.runnable.local.ui.components.StorageBreakdown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ModelDetailScreen(
    model: ModelRecord,
    status: ModelStatus,
    files: List<File>,
    dependencyNames: List<String>,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val expectedBytes = model.artifacts.sumOf { it.bytes }
    val actualBytes = files.sumOf { if (it.exists()) it.length() else 0L }
    val fileMap = files.associateBy { it.name }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (onBack != null) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = MaterialTheme.shapes.extraLarge
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
            }
        }

        SectionHeader(
            title = "Storage",
            subtitle = "Expected vs installed footprint."
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                StorageBreakdown(expectedBytes = expectedBytes, actualBytes = actualBytes)
            }
        }

        SectionHeader(
            title = "Artifacts",
            subtitle = "All files required for this model."
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            model.artifacts.forEach { artifact ->
                val file = fileMap[artifact.name]
                ArtifactRow(
                    name = artifact.name,
                    expectedBytes = artifact.bytes,
                    actualBytes = file?.length() ?: 0L
                )
            }
        }

        SectionHeader(
            title = "Dependencies",
            subtitle = "Download dependencies for correct runtime wiring."
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                DependencyGraph(modelName = model.name, dependencies = dependencyNames)
            }
        }

        SectionHeader(
            title = "Actions",
            subtitle = "Manage download state."
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                is ModelStatus.NotDownloaded, is ModelStatus.Failed -> {
                    Button(onClick = onDownload) { Text("Download") }
                }
                is ModelStatus.Downloading -> {
                    Text(
                        text = "Downloading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ModelStatus.Ready -> {
                    Button(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}
