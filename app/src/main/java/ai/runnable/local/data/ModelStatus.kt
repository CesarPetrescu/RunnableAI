package ai.runnable.local.data

import java.io.File

sealed class ModelStatus {
    data object NotDownloaded : ModelStatus()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ModelStatus()
    data class Ready(val files: List<File>) : ModelStatus()
    data class Failed(val message: String) : ModelStatus()
}
