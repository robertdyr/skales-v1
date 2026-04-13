package com.example.skales.audio

import com.example.skales.domain.model.Scale
import com.example.skales.domain.model.ScaleSoundKind

private const val DefaultNoteBreakAfterBeats = 1f
private const val DefaultCueBreakAfterBeats = 2f
private const val DefaultSetBreakAfterBeats = 1f

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

        val set = playableScale.sets[currentCursor.setIndex]
        val sound = set.sounds[currentCursor.soundIndexInSet]
        pianoSoundPlayer.playSound(sound.notes)

        val isLastSoundInSet = currentCursor.soundIndexInSet == set.sounds.lastIndex
        val isBoundarySet = when (direction) {
            PlaybackDirection.Forward -> currentCursor.setIndex == playableScale.sets.lastIndex
            PlaybackDirection.Backward -> currentCursor.setIndex == 0
        }
        val nextCursor = when {
            isLastSoundInSet && isBoundarySet -> PlaybackCursor(isFinished = true)
            isLastSoundInSet -> PlaybackCursor(
                setIndex = currentCursor.setIndex + direction.setDelta,
                soundIndexInSet = 0,
            )
            else -> currentCursor.copy(soundIndexInSet = currentCursor.soundIndexInSet + 1)
        }

        val soundBreak = sound.breakAfterBeats ?: defaultBreakAfter(sound.kind)
        val setBreak = if (isLastSoundInSet) {
            set.breakAfterBeats ?: DefaultSetBreakAfterBeats
        } else {
            0f
        }

        return StepResult(
            nextCursor = nextCursor,
            waitBeats = if (nextCursor.isFinished) 0f else soundBreak + setBreak,
            playedSetIndex = currentCursor.setIndex,
            playedSoundIndex = currentCursor.soundIndexInSet,
        )
    }

    private fun defaultBreakAfter(kind: ScaleSoundKind): Float {
        return when (kind) {
            ScaleSoundKind.Cue -> DefaultCueBreakAfterBeats
            ScaleSoundKind.Note -> DefaultNoteBreakAfterBeats
        }
    }
}

private val PlaybackDirection.setDelta: Int
    get() = when (this) {
        PlaybackDirection.Forward -> 1
        PlaybackDirection.Backward -> -1
    }

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
            PlaybackDirection.Backward -> PlaybackCursor(setIndex = playableScale.sets.lastIndex)
        }
    }

    val setIndex = setIndex.coerceIn(0, playableScale.sets.lastIndex)
    val set = playableScale.sets[setIndex]
    val soundIndex = soundIndexInSet.coerceIn(0, set.sounds.lastIndex)
    return PlaybackCursor(setIndex = setIndex, soundIndexInSet = soundIndex)
}
