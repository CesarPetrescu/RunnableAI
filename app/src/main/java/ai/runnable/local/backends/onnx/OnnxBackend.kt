package ai.runnable.local.backends.onnx

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.Result
import ai.onnxruntime.OrtSession.SessionOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class OnnxBackend(private val context: Context) {
    private val env = OrtEnvironment.getEnvironment()
    private val sessions = ConcurrentHashMap<String, OrtSession>()

    fun isAvailable(): Boolean = true

    suspend fun run(
        modelPath: String,
        inputs: Map<String, OnnxTensor>,
        useNnapi: Boolean = false
    ): Result = withContext(Dispatchers.IO) {
        val session = sessions.getOrPut(modelPath) { createSession(modelPath, useNnapi) }
        session.run(inputs)
    }

    fun close(modelPath: String) {
        sessions.remove(modelPath)?.close()
    }

    private fun createSession(modelPath: String, useNnapi: Boolean): OrtSession {
        val options = SessionOptions()
        if (useNnapi) {
            enableNnapi(options)
        }
        return env.createSession(modelPath, options)
    }

    private fun enableNnapi(options: SessionOptions) {
        runCatching {
            val method = SessionOptions::class.java.getMethod("addNnapi")
            method.invoke(options)
        }
    }
}
