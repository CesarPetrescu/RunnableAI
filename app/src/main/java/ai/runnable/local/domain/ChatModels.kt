package ai.runnable.local.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

@Serializable
data class ChatMessage(
    val role: ChatRole,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
