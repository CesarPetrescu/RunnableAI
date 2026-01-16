package ai.runnable.local.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File

class ModelCatalogRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = NetworkClient.client
    private val cacheFile = File(context.filesDir, "catalog.json")
    private val customFile = File(context.filesDir, "custom_models.json")

    suspend fun loadCatalog(remoteUrl: String? = null): ModelCatalog {
        return if (remoteUrl.isNullOrBlank()) {
            loadFromCacheOrAssets()
        } else {
            loadFromRemoteOrCache(remoteUrl)
        }
    }

    fun cachedCatalogOrNull(): ModelCatalog? {
        if (!cacheFile.exists() && !customFile.exists()) return null
        val base = if (cacheFile.exists()) {
            runCatching {
                val payload = cacheFile.readText()
                json.decodeFromString(ModelCatalog.serializer(), payload)
            }.getOrNull() ?: ModelCatalog()
        } else {
            ModelCatalog()
        }
        return mergeWithCustom(base)
    }

    private suspend fun loadFromCacheOrAssets(): ModelCatalog = withContext(Dispatchers.IO) {
        val base = cachedCatalogOrNull() ?: loadFromAssetsBase().also { writeCache(it) }
        mergeWithCustom(base)
    }

    private suspend fun loadFromRemoteOrCache(url: String): ModelCatalog = withContext(Dispatchers.IO) {
        runCatching {
            val base = loadFromRemoteBase(url).also { writeCache(it) }
            mergeWithCustom(base)
        }.getOrElse { error ->
            cachedCatalogOrNull() ?: throw error
        }
    }

    private fun loadFromAssetsBase(): ModelCatalog {
        context.assets.open("catalog.json").use { stream ->
            val payload = stream.readBytes().decodeToString()
            return json.decodeFromString(ModelCatalog.serializer(), payload)
        }
    }

    private fun loadFromRemoteBase(url: String): ModelCatalog {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Catalog fetch failed: ${response.code}")
            }
            val payload = response.body?.string().orEmpty()
            return json.decodeFromString(ModelCatalog.serializer(), payload)
        }
    }

    private fun writeCache(catalog: ModelCatalog) {
        val payload = json.encodeToString(ModelCatalog.serializer(), catalog)
        cacheFile.writeText(payload)
    }

    fun addCustomModel(model: ModelRecord) {
        val custom = readCustomCatalog()
        val merged = mergeModels(custom.models, listOf(model))
        writeCustomCatalog(ModelCatalog(models = merged))
    }

    fun removeCustomModel(modelId: String) {
        val custom = readCustomCatalog()
        val filtered = custom.models.filterNot { it.id == modelId }
        writeCustomCatalog(ModelCatalog(models = filtered))
    }

    private fun readCustomCatalog(): ModelCatalog {
        if (!customFile.exists()) return ModelCatalog()
        return runCatching {
            val payload = customFile.readText()
            json.decodeFromString(ModelCatalog.serializer(), payload)
        }.getOrDefault(ModelCatalog())
    }

    private fun writeCustomCatalog(catalog: ModelCatalog) {
        val payload = json.encodeToString(ModelCatalog.serializer(), catalog)
        customFile.writeText(payload)
    }

    private fun mergeWithCustom(base: ModelCatalog): ModelCatalog {
        val custom = readCustomCatalog()
        if (custom.models.isEmpty()) return base
        val merged = mergeModels(base.models, custom.models)
        return base.copy(models = merged)
    }

    private fun mergeModels(base: List<ModelRecord>, custom: List<ModelRecord>): List<ModelRecord> {
        val merged = LinkedHashMap<String, ModelRecord>()
        base.forEach { merged[it.id] = it }
        custom.forEach { merged[it.id] = it }
        return merged.values.toList()
    }
}
