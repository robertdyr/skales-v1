package com.example.skales.editor

import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import kotlin.math.roundToInt

data class SetGridNote(
    val soundId: String,
    val midi: Int,
    val column: Int,
)

data class SetGrid(
    val notes: List<SetGridNote>,
    val columnCount: Int,
    val minMidi: Int,
    val maxMidi: Int,
    val stepBeats: Float,
)

object SetGridOps {
    const val CoarseStepBeats = 1f
    const val DefaultStepBeats = 0.5f
    const val FineStepBeats = 0.25f
    const val DefaultMinMidi = 21
    const val DefaultMaxMidi = 108
    const val DefaultAdvanceBeats = 1f
    private const val MinimumColumnCount = 16

    fun toGrid(
        set: ScaleSet,
        stepBeats: Float = DefaultStepBeats,
        minMidi: Int = DefaultMinMidi,
        maxMidi: Int = DefaultMaxMidi,
    ): SetGrid {
        var currentColumn = 0
        val notes = mutableListOf<SetGridNote>()

        set.sounds.forEach { sound ->
            if (sound.kind == ScaleSoundKind.Note && sound.notes.isNotEmpty()) {
                notes += SetGridNote(
                    soundId = sound.id,
                    midi = sound.notes.first().coerceIn(minMidi, maxMidi),
                    column = currentColumn,
                )
                currentColumn += advanceToColumns(sound.breakAfterBeats, stepBeats)
            }
        }

        val farthestColumn = notes.maxOfOrNull { it.column + 1 } ?: 0
        return SetGrid(
            notes = notes,
            columnCount = maxOf(MinimumColumnCount, farthestColumn + 4),
            minMidi = minMidi,
            maxMidi = maxMidi,
            stepBeats = stepBeats,
        )
    }

    fun addNoteAtColumn(set: ScaleSet, midi: Int, column: Int, stepBeats: Float = DefaultStepBeats): ScaleSet {
        val newSound = ScaleSound(
            notes = listOf(midi),
            kind = ScaleSoundKind.Note,
            breakAfterBeats = DefaultAdvanceBeats,
        )
        val notePositions = extractNotePositions(set, stepBeats) + PositionedNote(
            soundId = newSound.id,
            sourceSound = newSound,
            midi = midi,
            column = column.coerceAtLeast(0),
        )
        return rebuildSet(set, notePositions, stepBeats)
    }

    fun moveNote(set: ScaleSet, soundId: String, midi: Int, column: Int, stepBeats: Float = DefaultStepBeats): ScaleSet {
        val notePositions = extractNotePositions(set, stepBeats).map { note ->
            if (note.soundId == soundId) {
                note.copy(
                    midi = midi,
                    column = column.coerceAtLeast(0),
                    sourceSound = note.sourceSound.copy(notes = listOf(midi)),
                )
            } else {
                note
            }
        }
        return rebuildSet(set, notePositions, stepBeats)
    }

    fun removeNote(set: ScaleSet, soundId: String, stepBeats: Float = DefaultStepBeats): ScaleSet {
        val remaining = extractNotePositions(set, stepBeats).filterNot { it.soundId == soundId }
        return rebuildSet(set, remaining, stepBeats)
    }

    fun nextFreeColumn(set: ScaleSet, stepBeats: Float = DefaultStepBeats): Int {
        val positionedNotes = extractNotePositions(set, stepBeats)
        val lastNote = positionedNotes.maxByOrNull { it.column } ?: return 0
        return lastNote.column + advanceToColumns(lastNote.sourceSound.breakAfterBeats, stepBeats)
    }

    private fun rebuildSet(set: ScaleSet, notePositions: List<PositionedNote>, stepBeats: Float): ScaleSet {
        val firstNoteIndex = set.sounds.indexOfFirst { it.kind == ScaleSoundKind.Note }
        val lastNoteIndex = set.sounds.indexOfLast { it.kind == ScaleSoundKind.Note }
        val leadingCues = if (firstNoteIndex > 0) {
            set.sounds.take(firstNoteIndex).filter { it.kind != ScaleSoundKind.Note }
        } else {
            emptyList()
        }
        val trailingCues = if (lastNoteIndex >= 0 && lastNoteIndex < set.sounds.lastIndex) {
            set.sounds.drop(lastNoteIndex + 1).filter { it.kind != ScaleSoundKind.Note }
        } else {
            emptyList()
        }
        val ordered = notePositions.sortedWith(compareBy<PositionedNote> { it.column }.thenBy { it.midi })
        val rebuiltNotes = ordered.mapIndexed { index, note ->
            val nextColumn = ordered.getOrNull(index + 1)?.column
            val advanceBeats = if (nextColumn == null) {
                note.sourceSound.breakAfterBeats ?: DefaultAdvanceBeats
            } else {
                columnsToAdvance((nextColumn - note.column).coerceAtLeast(1), stepBeats)
            }
            note.sourceSound.copy(
                notes = listOf(note.midi),
                breakAfterBeats = advanceBeats,
            )
        }

        return set.copy(sounds = leadingCues + rebuiltNotes + trailingCues)
    }

    private fun extractNotePositions(set: ScaleSet, stepBeats: Float): List<PositionedNote> {
        var currentColumn = 0
        return buildList {
            set.sounds.forEach { sound ->
                if (sound.kind == ScaleSoundKind.Note && sound.notes.isNotEmpty()) {
                    add(
                        PositionedNote(
                            soundId = sound.id,
                            sourceSound = sound,
                            midi = sound.notes.first(),
                            column = currentColumn,
                        ),
                    )
                    currentColumn += advanceToColumns(sound.breakAfterBeats, stepBeats)
                }
            }
        }
    }

    private fun advanceToColumns(breakAfterBeats: Float?, stepBeats: Float): Int {
        val beats = breakAfterBeats ?: DefaultAdvanceBeats
        return (beats / stepBeats).roundToInt().coerceAtLeast(1)
    }

    private fun columnsToAdvance(columns: Int, stepBeats: Float): Float {
        return columns * stepBeats
    }

    private data class PositionedNote(
        val soundId: String,
        val sourceSound: ScaleSound,
        val midi: Int,
        val column: Int,
    )
}
