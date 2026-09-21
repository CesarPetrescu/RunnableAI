package ai.runnable.local.asr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import org.jtransforms.fft.FloatFFT_1D
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CanaryAsrEngine(
    encoderPath: String,
    decoderPath: String,
    vocabPath: String,
    config: CanaryConfig
) {
    private val env = OrtEnvironment.getEnvironment()
    private val encoder: OrtSession = env.createSession(encoderPath, SessionOptions())
    private val decoder: OrtSession = env.createSession(decoderPath, SessionOptions())
    private val vocab = Vocab.fromFile(File(vocabPath))
    private val decoderInfo = DecoderInfo.fromSession(decoder)
    private val featuresSize = config.featuresSize
    private val maxSequenceLength = config.maxSequenceLength
    private val extractor = NemoLogMelExtractor(featuresSize)
    private val decodeSpaceRegex = Regex("\\A\\s|\\s\\B|(\\s)\\b")

    private val tokenStartContext = vocab.tokenId("<|startofcontext|>")
    private val tokenStartTranscript = vocab.tokenId("<|startoftranscript|>")
    private val tokenEmo = vocab.tokenId("<|emo:undefined|>")
    private val tokenLang = vocab.tokenId("<|en|>")
    private val tokenPnc = vocab.tokenId("<|pnc|>")
    private val tokenNoItN = vocab.tokenId("<|noitn|>")
    private val tokenNoTimestamp = vocab.tokenId("<|notimestamp|>")
    private val tokenNoDiarize = vocab.tokenId("<|nodiarize|>")
    private val tokenEos = vocab.tokenId("<|endoftext|>")

    fun transcribe(samples: FloatArray, sampleRate: Int): String {
        if (sampleRate != SAMPLE_RATE) {
            throw IllegalStateException("Expected ${SAMPLE_RATE}Hz audio, got ${sampleRate}Hz")
        }

        val features = extractor.extract(samples)
        val featuresTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(features.data),
            features.shape
        )
        val featuresLenTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(longArrayOf(features.length)),
            longArrayOf(1)
        )

        val encoderResult = encoder.run(
            mapOf(
                "audio_signal" to featuresTensor,
                "length" to featuresLenTensor
            )
        )
        val encoderEmbeddings = encoderResult[0] as OnnxTensor
        val encoderMask = encoderResult[1] as OnnxTensor

        var tokens = mutableListOf(
            tokenStartContext,
            tokenStartTranscript,
            tokenEmo,
            tokenLang,
            tokenLang,
            tokenPnc,
            tokenNoItN,
            tokenNoTimestamp,
            tokenNoDiarize
        )
        var decoderMems = FloatArray(0)
        var decoderMemsShape = longArrayOf(decoderInfo.memLayers.toLong(), 1, 0, decoderInfo.memHidden.toLong())

        while (tokens.size < maxSequenceLength) {
            val inputIds = if (decoderMemsShape[2] == 0L) tokens else listOf(tokens.last())
            val inputArray = LongArray(inputIds.size) { index -> inputIds[index].toLong() }
            val inputTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(inputArray),
                longArrayOf(1, inputArray.size.toLong())
            )
            val memTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(decoderMems),
                decoderMemsShape
            )

            val outputs = decoder.run(
                mapOf(
                    "input_ids" to inputTensor,
                    "encoder_embeddings" to encoderEmbeddings,
                    "encoder_mask" to encoderMask,
                    "decoder_mems" to memTensor
                )
            )
            val logitsTensor = outputs[0] as OnnxTensor
            val memsTensor = outputs[1] as OnnxTensor

            val nextToken = argmaxLastStep(logitsTensor)
            if (nextToken == tokenEos) {
                break
            }
            tokens.add(nextToken)

            decoderMemsShape = memsTensor.info.shape
            decoderMems = memsTensor.floatBuffer.toFloatArray()

            inputTensor.close()
            memTensor.close()
            logitsTensor.close()
            memsTensor.close()
            outputs.close()
        }

        featuresTensor.close()
        featuresLenTensor.close()
        encoderEmbeddings.close()
        encoderMask.close()
        encoderResult.close()

        val decodedTokens = tokens
            .drop(9)
            .mapNotNull { id ->
                val token = vocab.lookup(id)
                if (token.startsWith("<|")) null else token
            }
        val rawText = decodedTokens.joinToString("")
        return decodeSpaceRegex.replace(rawText) { match ->
            if (match.groups[1] != null) " " else ""
        }.trim()
    }

    private fun argmaxLastStep(logits: OnnxTensor): Int {
        val shape = logits.info.shape
        val vocabSize = shape.last().toInt()
        val data = logits.floatBuffer
        val stepOffset = (shape[0] * (shape[1] - 1) * vocabSize).toInt()
        var bestIndex = 0
        var bestValue = Float.NEGATIVE_INFINITY
        for (i in 0 until vocabSize) {
            val value = data[stepOffset + i]
            if (value > bestValue) {
                bestValue = value
                bestIndex = i
            }
        }
        return bestIndex
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }

    fun close() {
        encoder.close()
        decoder.close()
    }
}

@Serializable
data class CanaryConfig(
    @SerialName("features_size")
    val featuresSize: Int = 128,
    @SerialName("max_sequence_length")
    val maxSequenceLength: Int = 1024
) {
    companion object {
        fun load(file: File?): CanaryConfig {
            if (file == null || !file.exists()) return CanaryConfig()
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString(file.readText())
        }
    }
}

private data class DecoderInfo(
    val memLayers: Int,
    val memHidden: Int
) {
    companion object {
        fun fromSession(session: OrtSession): DecoderInfo {
            val memInfo = session.inputInfo["decoder_mems"]?.info as? ai.onnxruntime.TensorInfo
                ?: throw IllegalStateException("decoder_mems input not found")
            val shape = memInfo.shape
            return DecoderInfo(
                memLayers = shape[0].toInt(),
                memHidden = shape[3].toInt()
            )
        }
    }
}

private class Vocab(private val idToToken: Map<Int, String>) {
    fun lookup(id: Int): String = idToToken[id].orEmpty()

    fun tokenId(token: String): Int {
        return idToToken.entries.firstOrNull { it.value == token }?.key
            ?: throw IllegalStateException("Missing token: $token")
    }

    companion object {
        fun fromFile(file: File): Vocab {
            val map = mutableMapOf<Int, String>()
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach
                val lastSpace = trimmed.lastIndexOf(' ')
                if (lastSpace <= 0) return@forEach
                val token = trimmed.substring(0, lastSpace).replace("\u2581", " ")
                val id = trimmed.substring(lastSpace + 1).toIntOrNull() ?: return@forEach
                map[id] = token
            }
            return Vocab(map)
        }
    }
}

private data class FeatureBatch(
    val data: FloatArray,
    val shape: LongArray,
    val length: Long
)

private class NemoLogMelExtractor(private val featuresSize: Int) {
    private val nFft = 512
    private val winLength = 400
    private val hopLength = 160
    private val preEmphasis = 0.97f
    private val logGuard = 2.0.pow(-24.0).toFloat()
    private val window = buildWindow()
    private val melFilters = buildMelFilters()
    private val fft = FloatFFT_1D(nFft.toLong())

    fun extract(samples: FloatArray): FeatureBatch {
        val waveformsLen = samples.size
        val padded = FloatArray(waveformsLen + nFft)
        System.arraycopy(samples, 0, padded, nFft / 2, waveformsLen)

        for (i in padded.indices) {
            val idx = i - nFft / 2
            if (idx in samples.indices) {
                val prev = if (idx > 0) samples[idx - 1] else 0f
                padded[i] = samples[idx] - preEmphasis * prev
            }
        }

        val frames = waveformsLen / hopLength + 1
        val features = Array(featuresSize) { FloatArray(frames) }

        val frameBuffer = FloatArray(nFft)
        for (frame in 0 until frames) {
            val start = frame * hopLength
            for (i in 0 until nFft) {
                val sample = if (start + i < padded.size) padded[start + i] else 0f
                frameBuffer[i] = sample * window[i]
            }
            fft.realForward(frameBuffer)
            val power = FloatArray(nFft / 2 + 1)
            power[0] = frameBuffer[0] * frameBuffer[0]
            power[nFft / 2] = frameBuffer[1] * frameBuffer[1]
            for (k in 1 until nFft / 2) {
                val re = frameBuffer[2 * k]
                val im = frameBuffer[2 * k + 1]
                power[k] = re * re + im * im
            }
            for (m in 0 until featuresSize) {
                var sum = 0f
                val filter = melFilters[m]
                for (k in filter.indices) {
                    sum += filter[k] * power[k]
                }
                features[m][frame] = ln(max(sum + logGuard, logGuard))
            }
        }

        val featuresLen = waveformsLen / hopLength
        for (m in 0 until featuresSize) {
            val channel = features[m]
            if (featuresLen > 1) {
                var mean = 0f
                for (t in 0 until featuresLen) {
                    mean += channel[t]
                }
                mean /= featuresLen
                var variance = 0f
                for (t in 0 until featuresLen) {
                    val diff = channel[t] - mean
                    variance += diff * diff
                }
                variance /= (featuresLen - 1).toFloat()
                val denom = sqrt(variance) + 1e-5f
                for (t in 0 until featuresLen) {
                    channel[t] = (channel[t] - mean) / denom
                }
            }
            for (t in featuresLen until frames) {
                channel[t] = 0f
            }
        }

        val flattened = FloatArray(featuresSize * frames)
        var offset = 0
        for (m in 0 until featuresSize) {
            System.arraycopy(features[m], 0, flattened, offset, frames)
            offset += frames
        }

        return FeatureBatch(
            data = flattened,
            shape = longArrayOf(1, featuresSize.toLong(), frames.toLong()),
            length = featuresLen.toLong()
        )
    }

    private fun buildWindow(): FloatArray {
        val window = FloatArray(nFft)
        val pad = nFft / 2 - winLength / 2
        for (i in 0 until winLength) {
            val coeff = 0.5f - 0.5f * kotlin.math.cos(2.0 * Math.PI * i / (winLength - 1)).toFloat()
            window[i + pad] = coeff
        }
        return window
    }

    private fun buildMelFilters(): Array<FloatArray> {
        val melMin = hzToMel(0.0)
        val melMax = hzToMel(CanaryAsrEngine.SAMPLE_RATE / 2.0)
        val melPoints = DoubleArray(featuresSize + 2) { index ->
            melMin + (melMax - melMin) * index / (featuresSize + 1)
        }
        val hzPoints = melPoints.map { melToHz(it) }
        val bin = hzPoints.map { ((nFft + 1) * it / CanaryAsrEngine.SAMPLE_RATE).toInt() }
        val filters = Array(featuresSize) { FloatArray(nFft / 2 + 1) }
        for (m in 1..featuresSize) {
            val left = bin[m - 1]
            val center = bin[m]
            val right = bin[m + 1]
            for (k in left until center) {
                if (center != left && k in filters[m - 1].indices) {
                    filters[m - 1][k] = ((k - left).toFloat() / (center - left))
                }
            }
            for (k in center until right) {
                if (right != center && k in filters[m - 1].indices) {
                    filters[m - 1][k] = ((right - k).toFloat() / (right - center))
                }
            }
            val denom = hzPoints[m + 1] - hzPoints[m - 1]
            if (denom > 0) {
                val scale = (2.0 / denom).toFloat()
                for (k in filters[m - 1].indices) {
                    filters[m - 1][k] *= scale
                }
            }
        }
        return filters
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * kotlin.math.log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
}

private fun FloatBuffer.toFloatArray(): FloatArray {
    val buffer = duplicate()
    val out = FloatArray(buffer.remaining())
    buffer.get(out)
    return out
}
