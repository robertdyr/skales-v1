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
    private val soundPlayer: SoundPlayer,
) {
    suspend fun next(
        scale: Scale,
        cursor: PlaybackCursor,
        direction: PlaybackDirection,
    ): StepResult {
        val playableScale = scale.withoutEmptySets()
        val timeline = playableScale.timelineEvents()
        val currentIndex = timeline.nextPlayableIndex(cursor.normalizedFor(playableScale, direction), direction)
        require(currentIndex >= 0) { "Cannot step a finished scale" }

        val currentEvent = timeline[currentIndex]
        val currentGroup = timeline.eventsAtStep(currentEvent.step)
        soundPlayer.playSound(currentGroup.map { event ->
            playableScale.sets[event.setIndex].sounds[event.soundIndexInSet].midi
        })

        val nextEvent = timeline.nextGroupAnchor(currentEvent.step, direction)
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

private fun List<TimelineEvent>.nextPlayableIndex(cursor: PlaybackCursor, direction: PlaybackDirection): Int {
    if (isEmpty()) return -1

    val exactMatchIndex = indexOfFirst {
        it.setIndex == cursor.setIndex && it.soundIndexInSet == cursor.soundIndexInSet
    }
    if (exactMatchIndex >= 0) return exactMatchIndex

    return when (direction) {
        PlaybackDirection.Forward -> indexOfFirst {
            it.setIndex > cursor.setIndex ||
                (it.setIndex == cursor.setIndex && it.soundIndexInSet >= cursor.soundIndexInSet)
        }
        PlaybackDirection.Backward -> indexOfLast {
            it.setIndex < cursor.setIndex ||
                (it.setIndex == cursor.setIndex && it.soundIndexInSet <= cursor.soundIndexInSet)
        }
    }
}

private fun List<TimelineEvent>.eventsAtStep(step: Int): List<TimelineEvent> {
    return filter { it.step == step }
}

private fun List<TimelineEvent>.nextGroupAnchor(step: Int, direction: PlaybackDirection): TimelineEvent? {
    return when (direction) {
        PlaybackDirection.Forward -> firstOrNull { it.step > step }
        PlaybackDirection.Backward -> lastOrNull { it.step < step }
    }
}

private data class TimelineEvent(
    val setIndex: Int,
    val soundIndexInSet: Int,
    val step: Int,
)

fun Scale.withoutEmptySets(): Scale = copy(
    sets = sets
        .map { set -> set.copy(sounds = set.sounds) }
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
