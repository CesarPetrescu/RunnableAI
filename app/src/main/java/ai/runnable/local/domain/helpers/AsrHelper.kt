package ai.runnable.local.domain.helpers

import ai.runnable.local.data.ModelTask
import ai.runnable.local.data.RuntimeType
import ai.runnable.local.domain.helpers.runtime.OnnxRuntimeHelper

class AsrHelper(
    private val resolver: ModelResolver,
    private val onnxRuntime: OnnxRuntimeHelper
) {
    suspend fun recognize(modelId: String): Result<String> {
        return when (val resolved = resolver.resolveReady(modelId)) {
            is ResolveResult.Error -> Result.failure(IllegalStateException(resolved.message))
            is ResolveResult.Success -> {
                val model = resolved.model
                if (model.task != ModelTask.ASR || model.runtime != RuntimeType.ONNX) {
                    return Result.failure(IllegalStateException("Model is not an ONNX ASR model"))
                }
                Result.failure(IllegalStateException("ASR pipeline not wired yet"))
            }
        }
    }
}
