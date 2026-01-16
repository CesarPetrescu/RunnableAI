package ai.runnable.local.backends

import ai.runnable.local.backends.executorch.ExecuTorchBackend
import ai.runnable.local.backends.llama.LlamaBackend
import ai.runnable.local.backends.onnx.OnnxBackend
import ai.runnable.local.data.RuntimeType

class BackendRegistry(
    val llamaBackend: LlamaBackend,
    val onnxBackend: OnnxBackend,
    val execuTorchBackend: ExecuTorchBackend
) {
    fun runtimeAvailable(runtime: RuntimeType): Boolean {
        return when (runtime) {
            RuntimeType.LLAMA_CPP -> true
            RuntimeType.ONNX -> onnxBackend.isAvailable()
            RuntimeType.EXECUTORCH -> execuTorchBackend.isAvailable()
        }
    }
}
