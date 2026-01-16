package ai.runnable.local.ui.components

import kotlin.math.ln
import kotlin.math.pow

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val unit = 1024.0
    val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceAtMost(4)
    val value = bytes / unit.pow(exp.toDouble())
    val suffix = listOf("B", "KB", "MB", "GB", "TB")[exp]
    return String.format("%.1f %s", value, suffix)
}
