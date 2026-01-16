package ai.runnable.local.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class ModelDownloadManager(
    private val store: ModelStore,
    private val client: okhttp3.OkHttpClient = NetworkClient.client,
    private val authConfig: DownloadAuthConfig = DownloadAuthConfig()
) {
    suspend fun downloadModel(
        model: ModelRecord,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val modelDir = store.ensureModelDir(model)
            val totalBytesHint = model.artifacts.sumOf { if (it.bytes > 0) it.bytes else 0L }
            val progressByArtifact = ConcurrentHashMap<String, Long>()
            val totalByArtifact = ConcurrentHashMap<String, Long>()
            val progressLock = Any()

            val outputFiles = coroutineScope {
                model.artifacts.map { artifact ->
                    async {
                        val dest = File(modelDir, artifact.name)
                        downloadFile(artifact, dest) { fileDownloaded, fileTotal ->
                            synchronized(progressLock) {
                                progressByArtifact[artifact.name] = fileDownloaded
                                val total = when {
                                    fileTotal > 0 -> fileTotal
                                    artifact.bytes > 0 -> artifact.bytes
                                    else -> 0L
                                }
                                if (total > 0) {
                                    totalByArtifact[artifact.name] = total
                                }
                                val downloadedSum = progressByArtifact.values.sum()
                                val totalSum = if (totalBytesHint > 0) {
                                    totalBytesHint
                                } else {
                                    totalByArtifact.values.sum()
                                }
                                onProgress(downloadedSum, totalSum)
                            }
                        }
                        dest
                    }
                }.awaitAll()
            }

            Result.success(outputFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadFile(
        artifact: ModelArtifact,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Long {
        if (dest.exists() && artifact.sha256.isNotBlank()) {
            if (sha256(dest) == artifact.sha256.lowercase()) {
                onProgress(dest.length(), dest.length())
                return dest.length()
            }
        }

        dest.parentFile?.mkdirs()
        val part = File(dest.parentFile, "${dest.name}.part")
        val existingBytes = if (part.exists()) part.length() else 0L

        val requestBuilder = Request.Builder().url(artifact.url)
        if (existingBytes > 0L) {
            requestBuilder.addHeader("Range", "bytes=${existingBytes}-")
        }
        applyAuthHeaders(artifact.url, requestBuilder)
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val code = response.code
                if (code == 401 && isHuggingFaceUrl(artifact.url)) {
                    throw IllegalStateException("Download failed: 401 (Hugging Face token required or not authorized)")
                }
                throw IllegalStateException("Download failed: $code")
            }

            val contentLength = response.body?.contentLength() ?: 0L
            val total = if (contentLength > 0) contentLength + existingBytes else 0L

            val sink = FileOutputStream(part, existingBytes > 0L)
            response.body?.byteStream()?.source()?.buffer().use { source ->
                sink.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = source?.read(buffer) ?: -1
                    var written = existingBytes
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        written += read.toLong()
                        onProgress(written, total)
                        read = source?.read(buffer) ?: -1
                    }
                }
            }
        }

        if (dest.exists()) {
            dest.delete()
        }
        if (!part.renameTo(dest)) {
            throw IllegalStateException("Failed to finalize download for ${dest.name}")
        }

        if (artifact.sha256.isNotBlank()) {
            val actual = sha256(dest)
            if (actual != artifact.sha256.lowercase()) {
                dest.delete()
                throw IllegalStateException("SHA-256 mismatch for ${dest.name}")
            }
        }

        return dest.length()
    }

    private fun applyAuthHeaders(url: String, builder: Request.Builder) {
        val token = authConfig.huggingFaceToken?.trim().orEmpty()
        if (token.isBlank()) return
        if (isHuggingFaceUrl(url)) {
            builder.addHeader("Authorization", "Bearer $token")
        }
    }

    private fun isHuggingFaceUrl(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host ?: return false
        return host.endsWith("huggingface.co") || host.endsWith("hf.co")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = stream.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
