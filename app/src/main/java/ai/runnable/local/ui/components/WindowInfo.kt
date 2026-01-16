package ai.runnable.local.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded
}

@Immutable
data class WindowInfo(
    val widthClass: WindowWidthClass
)

@Composable
fun rememberWindowInfo(): WindowInfo {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val widthClass = when {
        width < 600 -> WindowWidthClass.Compact
        width < 840 -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }
    return remember(widthClass) { WindowInfo(widthClass) }
}
