package ai.runnable.local.ui

import ai.runnable.local.MainViewModel
import ai.runnable.local.R
import ai.runnable.local.ui.components.AppBottomBar
import ai.runnable.local.ui.components.AppTopBar
import ai.runnable.local.ui.components.GradientBackground
import ai.runnable.local.ui.components.NavItem
import ai.runnable.local.ui.screens.ChatScreen
import ai.runnable.local.ui.screens.ModelsScreen
import ai.runnable.local.ui.screens.VoiceScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        NavItem("Models", ImageVector.vectorResource(id = R.drawable.ic_models)),
        NavItem("Chat", ImageVector.vectorResource(id = R.drawable.ic_chat)),
        NavItem("Voice", ImageVector.vectorResource(id = R.drawable.ic_voice))
    )

    GradientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                AppTopBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            },
            bottomBar = {
                AppBottomBar(
                    items = tabs,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                AnimatedContent(
                    targetState = selectedIndex,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it * direction } + fadeIn())
                            .togetherWith(slideOutHorizontally { -it * direction } + fadeOut())
                    },
                    label = "screen"
                ) { target ->
                    when (target) {
                        0 -> ModelsScreen(viewModel)
                        1 -> ChatScreen(viewModel)
                        2 -> VoiceScreen(viewModel)
                    }
                }
            }
        }
    }
}
