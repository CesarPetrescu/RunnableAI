package ai.runnable.local

import ai.runnable.local.ui.MainScreen
import ai.runnable.local.ui.theme.RunnableTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as RunnableAIApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunnableTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
