package ai.runnable.local.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder

class HuggingFaceRepository(
    private val client: okhttp3.OkHttpClient = NetworkClient.client,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun searchGgufModels(
        query: String,
        limit: Int,
        token: String?,
        cursor: String? = null
    ): HfSearchResponse {
        if (query.isBlank()) return HfSearchResponse(emptyList(), null)
        return withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
            val cursorParam = cursor?.let { "&cursor=${URLEncoder.encode(it, Charsets.UTF_8.name())}" }.orEmpty()
            val url = "https://huggingface.co/api/models?search=$encoded&limit=$limit&sort=downloads&direction=-1$cursorParam"
            val request = Request.Builder().url(url).applyAuth(token).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Hugging Face search failed: ${response.code}")
                }
                val payload = response.body?.string().orEmpty()
                val results = json.decodeFromString(ListSerializer(HfModelSummary.serializer()), payload)
                val filtered = results.filter { it.isGguf }
                val nextCursor = extractNextCursor(response.header("Link"))
                HfSearchResponse(models = filtered, nextCursor = nextCursor)
            }
        }
    }

    suspend fun listGgufFiles(repoId: String, token: String?): HfFileListing {
        if (repoId.isBlank()) {
            return HfFileListing(emptyList(), revision = "main", gated = false, isPrivate = false)
        }
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
                val files = details.siblings
                    .map { it.rfilename }
                    .filter { it.endsWith(".gguf", ignoreCase = true) }
                val revision = details.sha?.takeIf { it.isNotBlank() }
                    ?: details.defaultBranch?.takeIf { it.isNotBlank() }
                    ?: "main"
                HfFileListing(
                    files = files,
                    revision = revision,
                    gated = details.gated,
                    isPrivate = details.private
                )
            }
        }
    }

    fun buildResolveUrl(repoId: String, filename: String, revision: String = "main"): String {
        val builder = "https://huggingface.co".toHttpUrl().newBuilder()
        repoId.split("/").filter { it.isNotBlank() }.forEach { builder.addPathSegment(it) }
        builder.addPathSegment("resolve")
        builder.addPathSegment(revision)
        filename.split("/").filter { it.isNotBlank() }.forEach { builder.addPathSegment(it) }
        return builder.build().toString()
    }

    private fun Request.Builder.applyAuth(token: String?): Request.Builder {
        val trimmed = token?.trim().orEmpty()
        if (trimmed.isNotBlank()) {
            addHeader("Authorization", "Bearer $trimmed")
        }
        return this
    }

    private fun extractNextCursor(linkHeader: String?): String? {
        if (linkHeader.isNullOrBlank()) return null
        val nextLink = linkHeader.split(",")
            .firstOrNull { it.contains("rel=\"next\"") }
            ?: return null
        val url = nextLink.substringAfter("<").substringBefore(">").trim()
        val cursor = url.toHttpUrlOrNull()?.queryParameter("cursor")
        return cursor?.takeIf { it.isNotBlank() }
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
    val private: Boolean = false,
    val sha: String? = null,
    @SerialName("default_branch")
    val defaultBranch: String? = null,
    val siblings: List<HfSibling> = emptyList()
)

@Serializable
data class HfSibling(
    val rfilename: String
)

data class HfSearchResponse(
    val models: List<HfModelSummary>,
    val nextCursor: String? = null
)

data class HfFileListing(
    val files: List<String>,
    val revision: String,
    val gated: Boolean,
    val isPrivate: Boolean
)
