package com.example.skales.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.skales.R
import kotlin.math.pow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PianoSoundPlayer(
    context: Context,
) {
    private val activeStreamIds = mutableSetOf<Int>()
    private val sampleAnchors: List<SampleAnchor>
    private val samplesReady = CompletableDeferred<Unit>()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .build()

    init {
        val pendingSoundIds = mutableSetOf<Int>()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                pendingSoundIds.remove(sampleId)
                if (pendingSoundIds.isEmpty() && !samplesReady.isCompleted) {
                    samplesReady.complete(Unit)
                }
            }
        }

        sampleAnchors = listOf(
            SampleAnchor(midi = 48, soundId = soundPool.load(context, R.raw.piano_c3, 1)),
            SampleAnchor(midi = 60, soundId = soundPool.load(context, R.raw.piano_c4, 1)),
            SampleAnchor(midi = 72, soundId = soundPool.load(context, R.raw.piano_c5, 1)),
        )
        pendingSoundIds += sampleAnchors.map { it.soundId }
    }

    suspend fun playSound(notes: List<Int>) = withContext(Dispatchers.IO) {
        if (notes.isEmpty()) return@withContext
        samplesReady.await()
        notes.forEach(::playNoteInternal)
    }

    fun stop() {
        activeStreamIds.toList().forEach(soundPool::stop)
        activeStreamIds.clear()
    }

    fun release() {
        stop()
        soundPool.release()
    }

    private fun playNoteInternal(midi: Int): Int {
        val anchor = sampleAnchors.minBy { kotlin.math.abs(it.midi - midi) }
        val rate = 2.0.pow((midi - anchor.midi) / 12.0).toFloat().coerceIn(0.5f, 2.0f)
        val streamId = soundPool.play(anchor.soundId, 1f, 1f, 1, 0, rate)
        if (streamId != 0) {
            activeStreamIds += streamId
        }
        return streamId
    }

    private data class SampleAnchor(
        val midi: Int,
        val soundId: Int,
    )
}
