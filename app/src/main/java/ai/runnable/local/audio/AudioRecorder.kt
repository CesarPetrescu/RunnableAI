package ai.runnable.local.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max

class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val channelCount: Int = 1
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null
    private var raf: RandomAccessFile? = null
    private var bytesWritten: Long = 0L
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recordingJob != null

    fun start(outputFile: File) {
        if (recordingJob != null) return
        outputFile.parentFile?.mkdirs()
        this.outputFile = outputFile
        bytesWritten = 0L
        raf = RandomAccessFile(outputFile, "rw").apply {
            setLength(0L)
            writeWavHeader(this, 0L)
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBuffer, sampleRate / 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        recorder = audioRecord
        audioRecord.startRecording()

        recordingJob = scope.launch {
            val buffer = ByteArray(bufferSize)
            while (recordingJob?.isActive == true) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    raf?.write(buffer, 0, read)
                    bytesWritten += read
                }
            }
        }
    }

    suspend fun stop(): RecordingResult = withContext(Dispatchers.IO) {
        val job = recordingJob
        recordingJob = null
        val audioRecord = recorder
        recorder = null
        audioRecord?.stop()
        audioRecord?.release()
        job?.cancel()
        job?.join()

        val dataBytes = bytesWritten
        val fileHandle = raf
        val finalFile = outputFile ?: throw IllegalStateException("Missing recording file")
        fileHandle?.channel?.force(true)
        if (fileHandle != null) {
            writeWavHeader(fileHandle, dataBytes)
            fileHandle.close()
        }
        raf = null
        outputFile = null
        val durationMs = if (dataBytes > 0) {
            (dataBytes * 1000L) / (sampleRate * channelCount * 2L)
        } else {
            0L
        }
        RecordingResult(
            file = finalFile,
            bytes = dataBytes,
            durationMs = durationMs,
            sampleRate = sampleRate,
            channelCount = channelCount
        )
    }

    private fun writeWavHeader(raf: RandomAccessFile, dataSize: Long) {
        val byteRate = sampleRate * channelCount * 2
        val totalDataLen = dataSize + 36
        raf.seek(0L)
        raf.writeBytes("RIFF")
        raf.writeIntLE(totalDataLen.toInt())
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeIntLE(16)
        raf.writeShortLE(1.toShort())
        raf.writeShortLE(channelCount.toShort())
        raf.writeIntLE(sampleRate)
        raf.writeIntLE(byteRate)
        raf.writeShortLE((channelCount * 2).toShort())
        raf.writeShortLE(16.toShort())
        raf.writeBytes("data")
        raf.writeIntLE(dataSize.toInt())
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        writeInt(Integer.reverseBytes(value))
    }

    private fun RandomAccessFile.writeShortLE(value: Short) {
        writeShort(java.lang.Short.reverseBytes(value).toInt())
    }
}

data class RecordingResult(
    val file: File,
    val bytes: Long,
    val durationMs: Long,
    val sampleRate: Int,
    val channelCount: Int
)
