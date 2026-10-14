package com.ditzy.nurquran.data

import android.media.AudioAttributes
import android.media.MediaPlayer

/** Streams verse recitation from the Al Quran Cloud audio CDN while the reader screen is open. */
class QuranAudioController {
    private var player: MediaPlayer? = null

    fun play(globalAyahNumber: Int, onStarted: (Int) -> Unit, onFinished: () -> Unit) {
        stop()
        val next = MediaPlayer()
        player = next
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            next.setDataSource("https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalAyahNumber.mp3")
            next.setOnPreparedListener { media ->
                media.start()
                onStarted(globalAyahNumber)
            }
            next.setOnCompletionListener {
                stop()
                onFinished()
            }
            next.setOnErrorListener { _, _, _ ->
                stop()
                onFinished()
                true
            }
            next.prepareAsync()
        } catch (_: Exception) {
            stop()
            onFinished()
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.reset() }
        runCatching { player?.release() }
        player = null
    }

    fun release() = stop()
}
