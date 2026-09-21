package ai.runnable.local.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class RecordingStore(context: Context) {
    private val json = Json { prettyPrint = true }
    private val baseDir = File(context.filesDir, "recordings")
    private val metaFile = File(baseDir, "recordings.json")

    fun list(): List<RecordingEntry> {
        if (!metaFile.exists()) return emptyList()
        return runCatching {
            val payload = metaFile.readText()
            json.decodeFromString(ListSerializer(RecordingEntry.serializer()), payload)
        }.getOrDefault(emptyList())
    }

    fun createRecordingFile(): File {
        baseDir.mkdirs()
        val id = UUID.randomUUID().toString()
        return File(baseDir, "rec-$id.wav")
    }

    fun add(entry: RecordingEntry) {
        val updated = list().toMutableList()
        updated.add(0, entry)
        write(updated)
    }

    fun updateTranscript(id: String, transcript: String) {
        val updated = list().map { entry ->
            if (entry.id == id) entry.copy(transcript = transcript) else entry
        }
        write(updated)
    }

    fun fileFor(entry: RecordingEntry): File {
        return File(baseDir, entry.fileName)
    }

    private fun write(entries: List<RecordingEntry>) {
        baseDir.mkdirs()
        val payload = json.encodeToString(ListSerializer(RecordingEntry.serializer()), entries)
        metaFile.writeText(payload)
    }
}

@Serializable
data class RecordingEntry(
    val id: String,
    val fileName: String,
    val createdAt: Long,
    val durationMs: Long,
    val transcript: String = ""
)
