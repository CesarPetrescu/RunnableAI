package ai.runnable.local.data

import kotlinx.serialization.Serializable

@Serializable
data class ModelInstallRecord(
    val modelId: String,
    val installedAt: Long,
    val artifacts: List<InstalledArtifact>
)

@Serializable
data class InstalledArtifact(
    val name: String,
    val sha256: String,
    val bytes: Long
)
