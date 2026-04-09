package com.example.skales.audio

import com.example.skales.domain.model.Scale
import kotlinx.coroutines.delay

class ScaleAutoPlayer(
    private val scaleStepper: ScaleStepper,
) {
    suspend fun play(
        scale: Scale,
        initialCursor: PlaybackCursor,
        bpmProvider: () -> Int,
        updateCursor: (PlaybackCursor) -> Unit,
    ) {
        var cursor = initialCursor.normalizedFor(scale)
        if (cursor.isFinished) {
            cursor = PlaybackCursor()
            updateCursor(cursor)
        }

        while (!cursor.isFinished) {
            val step = scaleStepper.next(scale, cursor)
            cursor = step.nextCursor
            updateCursor(cursor)

            if (!cursor.isFinished && step.waitBeats > 0f) {
                delay(beatsToMilliseconds(step.waitBeats, bpmProvider()))
            }
        }
    }

    private fun beatsToMilliseconds(beats: Float, bpm: Int): Long {
        val safeBpm = bpm.coerceAtLeast(1)
        return ((60_000f / safeBpm) * beats).toLong().coerceAtLeast(0L)
    }
}
