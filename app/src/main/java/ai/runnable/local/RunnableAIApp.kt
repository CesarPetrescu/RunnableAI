package ai.runnable.local

import ai.runnable.local.audio.AudioPlayer
import ai.runnable.local.backends.BackendRegistry
import ai.runnable.local.backends.executorch.ExecuTorchBackend
import ai.runnable.local.backends.llama.LlamaBackend
import ai.runnable.local.backends.onnx.OnnxBackend
import ai.runnable.local.data.ModelCatalogRepository
import ai.runnable.local.data.ModelManager
import ai.runnable.local.data.ModelStore
import ai.runnable.local.domain.InferenceOrchestrator
import ai.runnable.local.domain.helpers.AsrHelper
import ai.runnable.local.domain.helpers.ChatHelper
import ai.runnable.local.domain.helpers.ModelResolver
import ai.runnable.local.domain.helpers.TtsHelper
import ai.runnable.local.domain.helpers.runtime.ExecuTorchRuntimeHelper
import ai.runnable.local.domain.helpers.runtime.LlamaRuntimeHelper
import ai.runnable.local.domain.helpers.runtime.OnnxRuntimeHelper
import android.app.Application
import android.util.Log
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RunnableAIApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}

class AppContainer(private val app: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val modelStore = ModelStore(app)
    private val catalogRepository = ModelCatalogRepository(app)
    private val modelManager = ModelManager(app, modelStore, catalogRepository, appScope)

    private val llamaBackend = LlamaBackend(app)
    private val onnxBackend = OnnxBackend(app)
    private val execuTorchBackend = ExecuTorchBackend()

    private val backendRegistry = BackendRegistry(
        llamaBackend = llamaBackend,
        onnxBackend = onnxBackend,
        execuTorchBackend = execuTorchBackend
    )

    private val audioPlayer = AudioPlayer(app)

    private val resolver = ModelResolver(modelManager)
    private val llamaRuntime = LlamaRuntimeHelper(backendRegistry.llamaBackend)
    private val onnxRuntime = OnnxRuntimeHelper(backendRegistry.onnxBackend)
    private val execuTorchRuntime = ExecuTorchRuntimeHelper(backendRegistry.execuTorchBackend)

    private val chatHelper = ChatHelper(resolver, llamaRuntime)
    private val ttsHelper = TtsHelper(resolver, audioPlayer, llamaRuntime, onnxRuntime, execuTorchRuntime)
    private val asrHelper = AsrHelper(resolver, onnxRuntime)

    val orchestrator = InferenceOrchestrator(
        chatHelper = chatHelper,
        ttsHelper = ttsHelper,
        asrHelper = asrHelper
    )

    val models = modelManager
}
