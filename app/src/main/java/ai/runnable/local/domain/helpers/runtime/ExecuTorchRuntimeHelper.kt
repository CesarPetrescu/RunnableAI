package ai.runnable.local.domain.helpers.runtime

import ai.runnable.local.backends.executorch.ExecuTorchBackend
import org.pytorch.executorch.EValue

class ExecuTorchRuntimeHelper(private val backend: ExecuTorchBackend) {
    fun execute(modelPath: String, method: String = "forward", inputs: Array<EValue>): Array<EValue> {
        return backend.execute(modelPath, method, inputs)
    }
}
