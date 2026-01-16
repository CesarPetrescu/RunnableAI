package ai.runnable.local.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelCatalog(
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    val models: List<ModelRecord> = emptyList()
)

@Serializable
data class ModelRecord(
    val id: String,
    val name: String,
    val task: ModelTask,
    val runtime: RuntimeType,
    val artifacts: List<ModelArtifact>,
    val requirements: ModelRequirements? = null,
    @SerialName("depends_on")
    val dependsOn: List<String> = emptyList(),
    val notes: List<String> = emptyList()
)

@Serializable
data class ModelArtifact(
    val name: String,
    val url: String,
    val sha256: String = "",
    val bytes: Long = 0
)

@Serializable
data class ModelRequirements(
    @SerialName("min_ram_mb")
    val minRamMb: Int? = null,
    @SerialName("preferred_abi")
    val preferredAbi: String? = null
)

@Serializable
enum class ModelTask {
    CHAT,
    ASR,
    TTS,
    CODEC
}

@Serializable
enum class RuntimeType {
    LLAMA_CPP,
    ONNX,
    EXECUTORCH
}
