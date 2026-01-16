package ai.runnable.local.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class AudioPlayer(private val context: Context) {
    fun playSineWave(
        durationMs: Int,
        frequencyHz: Float,
        sampleRate: Int = 24000
    ): Int {
        val totalSamples = (durationMs / 1000f * sampleRate).toInt().coerceAtLeast(1)
        val samples = ShortArray(totalSamples)
        val twoPi = 2.0 * PI
        for (i in samples.indices) {
            val angle = twoPi * frequencyHz * i / sampleRate
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.2).toInt().toShort()
        }
        playPcm(samples, sampleRate)
        return samples.size
    }

    fun playPcm(samples: ShortArray, sampleRate: Int) {
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .build(),
            minBuffer.coerceAtLeast(samples.size * 2),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        track.play()
        track.write(samples, 0, samples.size)
        track.stop()
        track.release()
    }
}
