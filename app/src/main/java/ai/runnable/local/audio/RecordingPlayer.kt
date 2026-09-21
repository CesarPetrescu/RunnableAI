package ai.runnable.local.audio

import android.content.Context
import android.media.MediaPlayer
import java.io.File

class RecordingPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(file: File, onComplete: () -> Unit) {
        stop()
        val player = MediaPlayer()
        mediaPlayer = player
        player.setDataSource(file.absolutePath)
        player.setOnCompletionListener {
            stop()
            onComplete()
        }
        player.prepare()
        player.start()
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
