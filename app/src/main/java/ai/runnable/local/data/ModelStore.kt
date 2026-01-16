package ai.runnable.local.data

import android.content.Context
import java.io.File

class ModelStore(context: Context) {
    private val baseDir: File = File(context.filesDir, "models")

    fun modelDir(modelId: String): File = File(baseDir, modelId)

    fun artifactFile(model: ModelRecord, artifact: ModelArtifact): File {
        return File(modelDir(model.id), artifact.name)
    }

    fun artifactFiles(model: ModelRecord): List<File> {
        return model.artifacts.map { artifactFile(model, it) }
    }

    fun installMarker(model: ModelRecord): File {
        return File(modelDir(model.id), "installed.json")
    }

    fun ensureModelDir(model: ModelRecord): File {
        val dir = modelDir(model.id)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun removeModel(model: ModelRecord) {
        val dir = modelDir(model.id)
        if (!dir.exists()) return
        dir.walkBottomUp().forEach { it.delete() }
        dir.delete()
    }
}
