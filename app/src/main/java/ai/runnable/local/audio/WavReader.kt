package ai.runnable.local.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WavData(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int
)

object WavReader {
    fun readPcm16(file: File): WavData {
        val bytes = file.readBytes()
        if (bytes.size < 44) {
            throw IllegalArgumentException("WAV file too small: ${file.name}")
        }
        val riff = String(bytes, 0, 4)
        val wave = String(bytes, 8, 4)
        if (riff != "RIFF" || wave != "WAVE") {
            throw IllegalArgumentException("Invalid WAV header: ${file.name}")
        }

        var fmtChannels = 0
        var fmtSampleRate = 0
        var fmtBits = 0
        var fmtAudioFormat = 0
        var dataOffset = -1
        var dataSize = 0

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4)
            val chunkSize = readIntLE(bytes, offset + 4)
            val chunkStart = offset + 8
            if (chunkStart + chunkSize > bytes.size) {
                break
            }
            when (chunkId) {
                "fmt " -> {
                    fmtAudioFormat = readShortLE(bytes, chunkStart)
                    fmtChannels = readShortLE(bytes, chunkStart + 2)
                    fmtSampleRate = readIntLE(bytes, chunkStart + 4)
                    fmtBits = readShortLE(bytes, chunkStart + 14)
                }
                "data" -> {
                    dataOffset = chunkStart
                    dataSize = chunkSize
                    break
                }
            }
            offset = chunkStart + chunkSize
            if (chunkSize % 2 != 0) {
                offset += 1
            }
        }

        if (fmtAudioFormat != 1 || fmtBits != 16) {
            throw IllegalArgumentException("Unsupported WAV format for ${file.name}")
        }
        if (dataOffset <= 0 || dataSize <= 0) {
            throw IllegalArgumentException("WAV data chunk missing for ${file.name}")
        }

        val frameCount = dataSize / (2 * fmtChannels)
        val buffer = ByteBuffer.wrap(bytes, dataOffset, dataSize).order(ByteOrder.LITTLE_ENDIAN)
        val samples = FloatArray(frameCount)
        for (i in 0 until frameCount) {
            var sum = 0f
            for (ch in 0 until fmtChannels) {
                sum += buffer.short.toFloat()
            }
            val avg = sum / fmtChannels.toFloat()
            samples[i] = avg / 32768f
        }

        return WavData(
            samples = samples,
            sampleRate = fmtSampleRate,
            channels = fmtChannels
        )
    }

    private fun readIntLE(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            ((bytes[offset + 3].toInt() and 0xff) shl 24)
    }

    private fun readShortLE(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8)
    }
}
