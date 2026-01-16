package ai.runnable.local.domain.helpers.runtime

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import ai.runnable.local.backends.onnx.OnnxBackend

class OnnxRuntimeHelper(private val backend: OnnxBackend) {
    suspend fun run(modelPath: String, inputs: Map<String, OnnxTensor>, useNnapi: Boolean = false): OrtSession.Result {
        return backend.run(modelPath, inputs, useNnapi)
    }
}
