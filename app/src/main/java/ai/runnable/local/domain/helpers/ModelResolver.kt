package ai.runnable.local.domain.helpers

import ai.runnable.local.data.ModelManager
import ai.runnable.local.data.ModelRecord
import ai.runnable.local.data.ModelStatus
import java.io.File

sealed class ResolveResult {
    data class Success(val model: ModelRecord, val files: List<File>) : ResolveResult()
    data class Error(val message: String) : ResolveResult()
}

class ModelResolver(private val modelManager: ModelManager) {
    fun resolveReady(modelId: String): ResolveResult {
        val model = modelManager.catalog.value.firstOrNull { it.id == modelId }
            ?: return ResolveResult.Error("Unknown model: $modelId")
        val status = modelManager.statuses.value[modelId]
        if (status !is ModelStatus.Ready) {
            return ResolveResult.Error("Model not downloaded: $modelId")
        }
        val files = modelManager.modelFiles(modelId)
        if (files.isEmpty() || files.any { !it.exists() }) {
            return ResolveResult.Error("Model files missing")
        }
        return ResolveResult.Success(model, files)
    }
}
