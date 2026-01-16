package ai.runnable.local.ui.components

import ai.runnable.local.domain.ChatMessage
import ai.runnable.local.domain.ChatRole
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val colorScheme = MaterialTheme.colorScheme

    // Use proper theme colors with good contrast
    val (background, contentColor, labelColor) = when (message.role) {
        ChatRole.USER -> Triple(
            colorScheme.primary,
            colorScheme.onPrimary,
            colorScheme.onPrimary.copy(alpha = 0.7f)
        )
        ChatRole.ASSISTANT -> Triple(
            colorScheme.secondaryContainer,
            colorScheme.onSecondaryContainer,
            colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        ChatRole.SYSTEM -> Triple(
            colorScheme.errorContainer,
            colorScheme.onErrorContainer,
            colorScheme.onErrorContainer.copy(alpha = 0.7f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = background,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = messageLabel(message.role),
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
        }
    }
}

private fun messageLabel(role: ChatRole): String {
    return when (role) {
        ChatRole.USER -> "You"
        ChatRole.ASSISTANT -> "Assistant"
        ChatRole.SYSTEM -> "System"
    }
}
