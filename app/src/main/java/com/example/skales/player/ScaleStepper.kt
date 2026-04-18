package com.example.skales.player

import com.example.skales.model.Scale
import com.example.skales.editor.SetGridOps
import kotlin.math.absoluteValue

enum class PlaybackDirection {
    Forward,
    Backward,
}

data class PlaybackCursor(
    val setIndex: Int = 0,
    val soundIndexInSet: Int = 0,
    val isFinished: Boolean = false,
)

data class StepResult(
    val nextCursor: PlaybackCursor,
    val waitBeats: Float,
    val playedSetIndex: Int,
    val playedSoundIndex: Int,
)

class ScaleStepper(
    private val pianoSoundPlayer: PianoSoundPlayer,
) {
    suspend fun next(
        scale: Scale,
        cursor: PlaybackCursor,
        direction: PlaybackDirection,
    ): StepResult {
        val playableScale = scale.withoutEmptySets()
        val currentCursor = cursor.normalizedFor(playableScale, direction)
        require(!currentCursor.isFinished) { "Cannot step a finished scale" }

        val timeline = playableScale.timelineEvents()
        val currentIndex = timeline.indexOfFirst {
            it.setIndex == currentCursor.setIndex && it.soundIndexInSet == currentCursor.soundIndexInSet
        }
        require(currentIndex >= 0) { "Playback cursor does not point to a playable sound" }

        val currentEvent = timeline[currentIndex]
        val sound = playableScale.sets[currentEvent.setIndex].sounds[currentEvent.soundIndexInSet]
        pianoSoundPlayer.playSound(sound.notes)

        val nextIndex = currentIndex + direction.timelineDelta
        val nextEvent = timeline.getOrNull(nextIndex)
        val nextCursor = if (nextEvent == null) {
            PlaybackCursor(isFinished = true)
        } else {
            PlaybackCursor(setIndex = nextEvent.setIndex, soundIndexInSet = nextEvent.soundIndexInSet)
        }
        val waitBeats = if (nextEvent == null) {
            0f
        } else {
            ((nextEvent.step - currentEvent.step).absoluteValue * SetGridOps.InternalStepBeats)
        }

        return StepResult(
            nextCursor = nextCursor,
            waitBeats = waitBeats,
            playedSetIndex = currentEvent.setIndex,
            playedSoundIndex = currentEvent.soundIndexInSet,
        )
    }
}

private val PlaybackDirection.timelineDelta: Int
    get() = when (this) {
        PlaybackDirection.Forward -> 1
        PlaybackDirection.Backward -> -1
    }

private fun Scale.timelineEvents(): List<TimelineEvent> {
    return sets.flatMapIndexed { setIndex, set ->
        set.sounds.mapIndexed { soundIndexInSet, sound ->
            TimelineEvent(
                setIndex = setIndex,
                soundIndexInSet = soundIndexInSet,
                step = sound.step,
            )
        }
    }.sortedWith(compareBy<TimelineEvent> { it.step }.thenBy { it.setIndex }.thenBy { it.soundIndexInSet })
}

private data class TimelineEvent(
    val setIndex: Int,
    val soundIndexInSet: Int,
    val step: Int,
)

fun Scale.withoutEmptySets(): Scale = copy(
    sets = sets
        .map { set -> set.copy(sounds = set.sounds.filter { sound -> sound.notes.isNotEmpty() }) }
        .filter { it.sounds.isNotEmpty() },
)

fun PlaybackCursor.normalizedFor(scale: Scale, direction: PlaybackDirection = PlaybackDirection.Forward): PlaybackCursor {
    val playableScale = scale.withoutEmptySets()
    if (playableScale.sets.isEmpty()) return PlaybackCursor(isFinished = true)
    if (isFinished) {
        return when (direction) {
            PlaybackDirection.Forward -> PlaybackCursor()
            PlaybackDirection.Backward -> playableScale.sets.lastIndex.let { lastSetIndex ->
                PlaybackCursor(
                    setIndex = lastSetIndex,
                    soundIndexInSet = playableScale.sets[lastSetIndex].sounds.lastIndex,
                )
            }
        }
    }

    val setIndex = setIndex.coerceIn(0, playableScale.sets.lastIndex)
    val set = playableScale.sets[setIndex]
    val soundIndex = soundIndexInSet.coerceIn(0, set.sounds.lastIndex)
    return PlaybackCursor(setIndex = setIndex, soundIndexInSet = soundIndex)
}
