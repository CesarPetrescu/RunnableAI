package ai.runnable.local.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ModelManager(
    private val context: Context,
    private val store: ModelStore,
    private val catalogRepository: ModelCatalogRepository,
    private val scope: CoroutineScope
) {
    private val workManager by lazy { WorkManager.getInstance(context) }
    private val activeJobs = mutableMapOf<UUID, Job>()

    private val _catalog = MutableStateFlow<List<ModelRecord>>(emptyList())
    val catalog: StateFlow<List<ModelRecord>> = _catalog.asStateFlow()

    private val _statuses = MutableStateFlow<Map<String, ModelStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, ModelStatus>> = _statuses.asStateFlow()

    suspend fun refreshCatalog(remoteUrl: String? = null) {
        val catalog = catalogRepository.loadCatalog(remoteUrl)
        _catalog.value = catalog.models
        val statusMap = catalog.models.associate { model ->
            model.id to resolveStatus(model)
        }
        _statuses.value = statusMap
    }

    fun downloadModel(modelId: String) {
        val model = _catalog.value.firstOrNull { it.id == modelId } ?: return
        downloadDependencies(model)
        enqueueDownload(model)
    }

    fun addCustomModelAndDownload(model: ModelRecord) {
        catalogRepository.addCustomModel(model)
        upsertCatalogModel(model)
        enqueueDownload(model)
    }

    private fun downloadDependencies(model: ModelRecord) {
        model.dependsOn.forEach { dependencyId ->
            val dep = _catalog.value.firstOrNull { it.id == dependencyId } ?: return@forEach
            val status = _statuses.value[dependencyId]
            if (status !is ModelStatus.Ready) {
                enqueueDownload(dep)
            }
        }
    }

    private fun enqueueDownload(model: ModelRecord) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(ModelDownloadWorker.KEY_MODEL_ID to model.id))
            .addTag(downloadTag(model.id))
            .build()

        _statuses.update { it + (model.id to ModelStatus.Downloading(0f, 0L, 0L)) }

        workManager.enqueueUniqueWork(
            downloadWorkName(model.id),
            ExistingWorkPolicy.REPLACE,
            request
        )

        observeWork(request.id, model)
    }

    fun removeModel(modelId: String) {
        val model = _catalog.value.firstOrNull { it.id == modelId } ?: return
        store.removeModel(model)
        _statuses.update { it + (modelId to ModelStatus.NotDownloaded) }
    }

    fun resolveStatus(model: ModelRecord): ModelStatus {
        val files = store.artifactFiles(model)
        val marker = store.installMarker(model)
        val allPresent = files.isNotEmpty() && files.all { it.exists() }
        if (!allPresent) {
            return ModelStatus.NotDownloaded
        }
        if (!marker.exists()) {
            return ModelStatus.NotDownloaded
        }
        return ModelStatus.Ready(files)
    }

    fun modelFiles(modelId: String): List<File> {
        val model = _catalog.value.firstOrNull { it.id == modelId } ?: return emptyList()
        return store.artifactFiles(model)
    }

    private fun observeWork(workId: UUID, model: ModelRecord) {
        activeJobs.remove(workId)?.cancel()
        val job = scope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { info ->
                val progress = info.progress
                val downloaded = progress.getLong(ModelDownloadWorker.KEY_DOWNLOADED, 0L)
                val total = progress.getLong(ModelDownloadWorker.KEY_TOTAL, 0L)
                val ratio = if (total > 0) downloaded.toFloat() / total else 0f

                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        _statuses.update {
                            it + (model.id to ModelStatus.Downloading(ratio, downloaded, total))
                        }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val files = store.artifactFiles(model)
                        _statuses.update { it + (model.id to ModelStatus.Ready(files)) }
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        val error = info.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                            ?: "Download failed"
                        _statuses.update { it + (model.id to ModelStatus.Failed(error)) }
                    }
                    else -> Unit
                }

                if (info.state.isFinished) {
                    activeJobs.remove(workId)
                    cancel()
                }
            }
        }
        activeJobs[workId] = job
    }

    private fun downloadTag(modelId: String) = "download-$modelId"
    private fun downloadWorkName(modelId: String) = "download-$modelId"

    private fun upsertCatalogModel(model: ModelRecord) {
        _catalog.update { current ->
            val filtered = current.filterNot { it.id == model.id }
            filtered + model
        }
        _statuses.update { it + (model.id to resolveStatus(model)) }
    }
}
