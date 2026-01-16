package ai.runnable.local.ui.components

import ai.runnable.local.data.ModelRecord
import ai.runnable.local.data.ModelStatus
import ai.runnable.local.ui.theme.RunnableTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ModelPicker(
    label: String,
    models: List<ModelRecord>,
    statuses: Map<String, ModelStatus>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val selected = models.firstOrNull { it.id == selectedId }
    val colors = RunnableTheme.colors

    // Picker button
    Surface(
        onClick = { showDialog = true },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selected?.name ?: "None selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Selection dialog
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (models.isEmpty()) {
                        Text(
                            text = "No models available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(models, key = { it.id }) { model ->
                                val status = statuses[model.id] ?: ModelStatus.NotDownloaded
                                val isSelected = model.id == selectedId
                                
                                ModelPickerItem(
                                    model = model,
                                    status = status,
                                    isSelected = isSelected,
                                    onClick = {
                                        onSelect(model.id)
                                        showDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelPickerItem(
    model: ModelRecord,
    status: ModelStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = RunnableTheme.colors
    
    Surface(
        onClick = onClick,
        color = if (isSelected) colors.accentMuted else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) colors.onAccent else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusLabel(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(status)
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = colors.onAccent
                )
            }
        }
    }
}

private fun statusLabel(status: ModelStatus): String {
    return when (status) {
        is ModelStatus.NotDownloaded -> "Not downloaded"
        is ModelStatus.Downloading -> "Downloading ${(status.progress * 100).toInt()}%"
        is ModelStatus.Ready -> "Ready"
        is ModelStatus.Failed -> "Failed"
    }
}

@Composable
private fun statusColor(status: ModelStatus) = when (status) {
    is ModelStatus.NotDownloaded -> MaterialTheme.colorScheme.onSurfaceVariant
    is ModelStatus.Downloading -> MaterialTheme.colorScheme.primary
    is ModelStatus.Ready -> RunnableTheme.colors.success
    is ModelStatus.Failed -> MaterialTheme.colorScheme.error
}
