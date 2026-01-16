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

    suspend fun loadCatalog(remoteUrl: String? = null): ModelCatalog {
        return if (remoteUrl.isNullOrBlank()) {
            loadFromCacheOrAssets()
        } else {
            loadFromRemoteOrCache(remoteUrl)
        }
    }

    fun cachedCatalogOrNull(): ModelCatalog? {
        if (!cacheFile.exists()) return null
        return runCatching {
            val payload = cacheFile.readText()
            json.decodeFromString(ModelCatalog.serializer(), payload)
        }.getOrNull()
    }

    private suspend fun loadFromCacheOrAssets(): ModelCatalog = withContext(Dispatchers.IO) {
        cachedCatalogOrNull() ?: loadFromAssets().also { writeCache(it) }
    }

    private suspend fun loadFromRemoteOrCache(url: String): ModelCatalog = withContext(Dispatchers.IO) {
        runCatching {
            loadFromRemote(url).also { writeCache(it) }
        }.getOrElse { error ->
            cachedCatalogOrNull() ?: throw error
        }
    }

    private fun loadFromAssets(): ModelCatalog {
        context.assets.open("catalog.json").use { stream ->
            val payload = stream.readBytes().decodeToString()
            return json.decodeFromString(ModelCatalog.serializer(), payload)
        }
    }

    private fun loadFromRemote(url: String): ModelCatalog {
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
}
