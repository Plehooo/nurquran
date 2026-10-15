package com.ditzy.nurquran.data

import android.media.AudioAttributes
import android.media.MediaPlayer

/** Streams verse recitation from Al Quran Cloud. Optional sequence mode plays a whole surah in order. */
class QuranAudioController {
    private var player: MediaPlayer? = null
    private var sequence: List<Int> = emptyList()
    private var sequenceIndex = 0
    private var onStarted: (Int) -> Unit = {}
    private var onFinished: () -> Unit = {}
    private var generation = 0

    fun play(globalAyahNumber: Int, onStarted: (Int) -> Unit, onFinished: () -> Unit) {
        playSequence(listOf(globalAyahNumber), globalAyahNumber, onStarted, onFinished)
    }

    fun playSequence(
        globalAyahNumbers: List<Int>,
        startAtGlobal: Int,
        onStarted: (Int) -> Unit,
        onFinished: () -> Unit
    ) {
        stop()
        val safeSequence = globalAyahNumbers.filter { it in 1..6236 }.distinct()
        if (safeSequence.isEmpty()) {
            onFinished()
            return
        }
        sequence = safeSequence
        sequenceIndex = safeSequence.indexOf(startAtGlobal).takeIf { it >= 0 } ?: 0
        this.onStarted = onStarted
        this.onFinished = onFinished
        startCurrentAyah(generation)
    }

    private fun startCurrentAyah(token: Int) {
        if (token != generation || sequenceIndex !in sequence.indices) return
        val globalAyahNumber = sequence[sequenceIndex]
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
                if (token != generation || player !== media) {
                    runCatching { media.release() }
                    return@setOnPreparedListener
                }
                media.start()
                onStarted(globalAyahNumber)
            }
            next.setOnCompletionListener { media ->
                if (token != generation || player !== media) return@setOnCompletionListener
                runCatching { media.release() }
                player = null
                sequenceIndex++
                if (sequenceIndex < sequence.size) {
                    startCurrentAyah(token)
                } else {
                    sequence = emptyList()
                    sequenceIndex = 0
                    onFinished()
                }
            }
            next.setOnErrorListener { media, _, _ ->
                if (token == generation && player === media) {
                    runCatching { media.release() }
                    player = null
                    sequence = emptyList()
                    sequenceIndex = 0
                    onFinished()
                }
                true
            }
            next.prepareAsync()
        } catch (_: Exception) {
            runCatching { next.release() }
            if (player === next) player = null
            if (token == generation) {
                sequence = emptyList()
                sequenceIndex = 0
                onFinished()
            }
        }
    }

    fun stop() {
        generation++
        sequence = emptyList()
        sequenceIndex = 0
        runCatching { player?.stop() }
        runCatching { player?.reset() }
        runCatching { player?.release() }
        player = null
    }

    fun release() = stop()
}
