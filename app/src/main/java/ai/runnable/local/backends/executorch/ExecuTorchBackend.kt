package ai.runnable.local.backends.executorch

import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.util.concurrent.ConcurrentHashMap

class ExecuTorchBackend {
    private val modules = ConcurrentHashMap<String, Module>()

    fun isAvailable(): Boolean = true

    fun loadModule(modelPath: String): Module {
        return modules.getOrPut(modelPath) {
            Module.load(modelPath, Module.LOAD_MODE_MMAP)
        }
    }

    fun unload(modelPath: String) {
        modules.remove(modelPath)
    }

    fun execute(modelPath: String, method: String = "forward", inputs: Array<EValue>): Array<EValue> {
        val module = loadModule(modelPath)
        return module.execute(method, *inputs)
    }

    fun tensorFromFloatArray(data: FloatArray, shape: LongArray): Tensor {
        return Tensor.fromBlob(data, shape)
    }

    fun tensorFromLongArray(data: LongArray, shape: LongArray): Tensor {
        return Tensor.fromBlob(data, shape)
    }
}
