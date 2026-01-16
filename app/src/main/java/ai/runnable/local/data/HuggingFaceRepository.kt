package ai.runnable.local.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.net.URLEncoder

class HuggingFaceRepository(
    private val client: okhttp3.OkHttpClient = NetworkClient.client,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun searchGgufModels(query: String, limit: Int, token: String?): List<HfModelSummary> {
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
            val url = "https://huggingface.co/api/models?search=$encoded&limit=$limit&sort=downloads&direction=-1"
            val request = Request.Builder().url(url).applyAuth(token).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Hugging Face search failed: ${response.code}")
                }
                val payload = response.body?.string().orEmpty()
                val results = json.decodeFromString(ListSerializer(HfModelSummary.serializer()), payload)
                results.filter { it.isGguf }
            }
        }
    }

    suspend fun listGgufFiles(repoId: String, token: String?): List<String> {
        if (repoId.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val url = "https://huggingface.co/api/models/${repoId.trim()}"
            val request = Request.Builder().url(url).applyAuth(token).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 401) {
                        throw IllegalStateException("Hugging Face auth required or access not granted.")
                    }
                    throw IllegalStateException("Hugging Face model lookup failed: ${response.code}")
                }
                val payload = response.body?.string().orEmpty()
                val details = json.decodeFromString(HfModelDetails.serializer(), payload)
                details.siblings
                    .map { it.rfilename }
                    .filter { it.endsWith(".gguf", ignoreCase = true) }
            }
        }
    }

    fun buildResolveUrl(repoId: String, filename: String, revision: String = "main"): String {
        return "https://huggingface.co/$repoId/resolve/$revision/$filename"
    }

    private fun Request.Builder.applyAuth(token: String?): Request.Builder {
        val trimmed = token?.trim().orEmpty()
        if (trimmed.isNotBlank()) {
            addHeader("Authorization", "Bearer $trimmed")
        }
        return this
    }
}

@Serializable
data class HfModelSummary(
    val id: String,
    @SerialName("modelId")
    val modelId: String? = null,
    val tags: List<String> = emptyList(),
    val downloads: Long? = null,
    val likes: Int? = null,
    @SerialName("library_name")
    val libraryName: String? = null,
    @SerialName("pipeline_tag")
    val pipelineTag: String? = null
) {
    val isGguf: Boolean
        get() = tags.any { it.equals("gguf", ignoreCase = true) }
            || libraryName?.equals("gguf", ignoreCase = true) == true
}

@Serializable
data class HfModelDetails(
    val id: String,
    val gated: Boolean = false,
    val siblings: List<HfSibling> = emptyList()
)

@Serializable
data class HfSibling(
    val rfilename: String
)
