package ai.runnable.local.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.serialization.json.Json
import java.io.File

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val repository = ModelCatalogRepository(applicationContext)
        val catalog = repository.cachedCatalogOrNull() ?: return Result.failure(
            Data.Builder().putString(KEY_ERROR, "Missing cached catalog").build()
        )

        val model = catalog.models.firstOrNull { it.id == modelId }
            ?: return Result.failure(Data.Builder().putString(KEY_ERROR, "Unknown model").build())

        val store = ModelStore(applicationContext)
        val settings = AppSettings(applicationContext)
        val downloader = ModelDownloadManager(
            store = store,
            authConfig = DownloadAuthConfig(huggingFaceToken = settings.huggingFaceToken)
        )

        val result = downloader.downloadModel(model) { downloaded, total ->
            val progress = Data.Builder()
                .putLong(KEY_DOWNLOADED, downloaded)
                .putLong(KEY_TOTAL, total)
                .build()
            setProgressAsync(progress)
        }

        return result.fold(
            onSuccess = { files ->
                writeInstallRecord(model, files, store)
                Result.success()
            },
            onFailure = { error ->
                Result.failure(Data.Builder().putString(KEY_ERROR, error.message).build())
            }
        )
    }

    private fun writeInstallRecord(model: ModelRecord, files: List<File>, store: ModelStore) {
        val artifactMap = files.associateBy { it.name }
        val record = ModelInstallRecord(
            modelId = model.id,
            installedAt = System.currentTimeMillis(),
            artifacts = model.artifacts.map { artifact ->
                val file = artifactMap[artifact.name]
                InstalledArtifact(
                    name = artifact.name,
                    sha256 = artifact.sha256,
                    bytes = file?.length() ?: 0L
                )
            }
        )
        val json = Json { prettyPrint = true }
        val marker = store.installMarker(model)
        marker.writeText(json.encodeToString(ModelInstallRecord.serializer(), record))
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_DOWNLOADED = "downloaded"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
    }
}
