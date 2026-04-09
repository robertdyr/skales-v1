package com.example.skales.audio

import com.example.skales.domain.model.Scale
import com.example.skales.domain.model.ScaleSoundKind

private const val DefaultNoteBreakAfterBeats = 1f
private const val DefaultCueBreakAfterBeats = 2f
private const val DefaultSetBreakAfterBeats = 1f

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
    ): StepResult {
        val playableScale = scale.withoutEmptySets()
        val currentCursor = cursor.normalizedFor(playableScale)
        require(!currentCursor.isFinished) { "Cannot step a finished scale" }

        val set = playableScale.sets[currentCursor.setIndex]
        val sound = set.sounds[currentCursor.soundIndexInSet]
        pianoSoundPlayer.playSound(sound.notes)

        val isLastSoundInSet = currentCursor.soundIndexInSet == set.sounds.lastIndex
        val isLastSet = currentCursor.setIndex == playableScale.sets.lastIndex
        val nextCursor = when {
            isLastSoundInSet && isLastSet -> PlaybackCursor(isFinished = true)
            isLastSoundInSet -> PlaybackCursor(setIndex = currentCursor.setIndex + 1)
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

fun Scale.withoutEmptySets(): Scale = copy(
    sets = sets
        .map { set -> set.copy(sounds = set.sounds.filter { sound -> sound.notes.isNotEmpty() }) }
        .filter { it.sounds.isNotEmpty() },
)

fun PlaybackCursor.normalizedFor(scale: Scale): PlaybackCursor {
    val playableScale = scale.withoutEmptySets()
    if (playableScale.sets.isEmpty()) return PlaybackCursor(isFinished = true)
    if (isFinished) return PlaybackCursor()

    val setIndex = setIndex.coerceIn(0, playableScale.sets.lastIndex)
    val set = playableScale.sets[setIndex]
    val soundIndex = soundIndexInSet.coerceIn(0, set.sounds.lastIndex)
    return PlaybackCursor(setIndex = setIndex, soundIndexInSet = soundIndex)
}
