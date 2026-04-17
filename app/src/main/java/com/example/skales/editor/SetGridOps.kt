package com.example.skales.editor

import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import kotlin.math.roundToInt

data class SetGridNote(
    val soundId: String,
    val midis: List<Int>,
    val column: Int,
    val setIndex: Int = 0,
    val kind: ScaleSoundKind = ScaleSoundKind.Note,
)

data class SetGrid(
    val notes: List<SetGridNote>,
    val columnCount: Int,
    val minMidi: Int,
    val maxMidi: Int,
    val stepBeats: Float,
    val setStartColumns: List<Int> = listOf(0),
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
        return toGrid(listOf(set), stepBeats, minMidi, maxMidi)
    }

    fun toGrid(
        sets: List<ScaleSet>,
        stepBeats: Float = DefaultStepBeats,
        minMidi: Int = DefaultMinMidi,
        maxMidi: Int = DefaultMaxMidi,
    ): SetGrid {
        val normalizedSets = if (sets.isEmpty()) listOf(ScaleSet(sounds = emptyList())) else sets
        val notes = mutableListOf<SetGridNote>()
        val setStartColumns = mutableListOf<Int>()
        var currentColumn = 0

        normalizedSets.forEachIndexed { setIndex, set ->
            setStartColumns += currentColumn
            val positionedSounds = extractSoundPositions(set, stepBeats)

            positionedSounds.forEach { note ->
                notes += SetGridNote(
                    soundId = note.soundId,
                    midis = note.sourceSound.notes.map { midi -> midi.coerceIn(minMidi, maxMidi) },
                    column = currentColumn + note.column,
                    setIndex = setIndex,
                    kind = note.sourceSound.kind,
                )
            }

            currentColumn += nextFreeColumn(set, stepBeats)
        }

        val farthestColumn = notes.maxOfOrNull { it.column + 1 } ?: 0
        return SetGrid(
            notes = notes,
            columnCount = maxOf(MinimumColumnCount, farthestColumn + 4),
            minMidi = minMidi,
            maxMidi = maxMidi,
            stepBeats = stepBeats,
            setStartColumns = setStartColumns.ifEmpty { listOf(0) },
        )
    }

    fun addNoteAtColumn(set: ScaleSet, midi: Int, column: Int, stepBeats: Float = DefaultStepBeats): ScaleSet {
        val newSound = ScaleSound(
            notes = listOf(midi),
            kind = ScaleSoundKind.Note,
            breakAfterBeats = DefaultAdvanceBeats,
        )
        val notePositions = extractSoundPositions(set, stepBeats) + PositionedSound(
            soundId = newSound.id,
            sourceSound = newSound,
            midi = midi,
            column = column.coerceAtLeast(0),
            originalOrder = set.sounds.size,
        )
        return rebuildSet(set, notePositions, stepBeats)
    }

    fun moveNote(set: ScaleSet, soundId: String, midi: Int, column: Int, stepBeats: Float = DefaultStepBeats): ScaleSet {
        val notePositions = extractSoundPositions(set, stepBeats).map { note ->
            if (note.soundId == soundId) {
                note.copy(
                    midi = midi,
                    column = column.coerceAtLeast(0),
                    sourceSound = note.sourceSound.copy(notes = transposeNotes(note.sourceSound.notes, note.midi, midi)),
                )
            } else {
                note
            }
        }
        return rebuildSet(set, notePositions, stepBeats)
    }

    fun moveSoundInTimeline(
        sets: List<ScaleSet>,
        selectedSetIndex: Int,
        soundId: String,
        midi: Int,
        column: Int,
        stepBeats: Float = DefaultStepBeats,
    ): List<ScaleSet> {
        val normalizedSets = if (sets.isEmpty()) listOf(ScaleSet(sounds = emptyList())) else sets
        val positionedSounds = extractTimelinePositions(normalizedSets, stepBeats)
        val target = positionedSounds.firstOrNull { it.soundId == soundId && it.setIndex == selectedSetIndex } ?: return normalizedSets
        val currentSetStart = positionedSounds
            .filter { it.setIndex == selectedSetIndex }
            .minOfOrNull { it.column }
            ?: 0
        val previousSetLast = positionedSounds
            .filter { it.setIndex == selectedSetIndex - 1 }
            .maxByOrNull { it.column }
        val nextSetStart = positionedSounds
            .filter { it.setIndex == selectedSetIndex + 1 }
            .minOfOrNull { it.column }

        val movesCurrentSetBoundary = selectedSetIndex > 0 && target.column == currentSetStart

        val previousSameSet = positionedSounds
            .filter { it.setIndex == selectedSetIndex && it.soundId != soundId && it.column < target.column }
            .maxByOrNull { it.column }
        val nextSameSet = positionedSounds
            .filter { it.setIndex == selectedSetIndex && it.soundId != soundId && it.column > target.column }
            .minByOrNull { it.column }

        val minColumn = if (movesCurrentSetBoundary) {
            (previousSetLast?.column ?: -1) + 1
        } else {
            (previousSameSet?.column ?: -1) + 1
        }
        val maxColumn = nextSameSet?.column?.minus(1) ?: Int.MAX_VALUE
        val clampedColumn = column.coerceIn(minColumn, maxColumn.coerceAtLeast(minColumn))

        val crossedBoundary = nextSetStart != null && clampedColumn >= nextSetStart
        val boundaryShift = if (crossedBoundary) (clampedColumn + 1) - nextSetStart else 0

        val updated = positionedSounds.map { sound ->
            when {
                sound.soundId == soundId && sound.setIndex == selectedSetIndex -> {
                    sound.copy(
                        midi = midi,
                        column = clampedColumn,
                        sourceSound = sound.sourceSound.copy(notes = transposeNotes(sound.sourceSound.notes, sound.midi, midi)),
                    )
                }
                crossedBoundary && sound.setIndex > selectedSetIndex -> {
                    sound.copy(column = sound.column + boundaryShift)
                }
                else -> sound
            }
        }

        return rebuildTimelineSets(normalizedSets, updated, stepBeats)
    }

    fun moveSetBoundaryInTimeline(
        sets: List<ScaleSet>,
        targetSetIndex: Int,
        startColumn: Int,
        stepBeats: Float = DefaultStepBeats,
    ): List<ScaleSet> {
        val normalizedSets = if (sets.isEmpty()) listOf(ScaleSet(sounds = emptyList())) else sets
        if (targetSetIndex <= 0 || targetSetIndex > normalizedSets.lastIndex) return normalizedSets

        val positionedSounds = extractTimelinePositions(normalizedSets, stepBeats)
        val currentSetStart = positionedSounds
            .filter { it.setIndex == targetSetIndex }
            .minOfOrNull { it.column }
            ?: return normalizedSets
        val previousSetLast = positionedSounds
            .filter { it.setIndex == targetSetIndex - 1 }
            .maxByOrNull { it.column }
            ?: return normalizedSets

        val clampedStart = startColumn.coerceAtLeast(previousSetLast.column + 1)
        val delta = clampedStart - currentSetStart
        if (delta == 0) return normalizedSets

        val shifted = positionedSounds.map { sound ->
            if (sound.setIndex >= targetSetIndex) {
                sound.copy(column = sound.column + delta)
            } else {
                sound
            }
        }

        return rebuildTimelineSets(normalizedSets, shifted, stepBeats)
    }

    fun removeNote(set: ScaleSet, soundId: String, stepBeats: Float = DefaultStepBeats): ScaleSet {
        val remaining = extractSoundPositions(set, stepBeats).filterNot { it.soundId == soundId }
        return rebuildSet(set, remaining, stepBeats)
    }

    fun nextFreeColumn(set: ScaleSet, stepBeats: Float = DefaultStepBeats): Int {
        val positionedNotes = extractSoundPositions(set, stepBeats)
        val lastNote = positionedNotes.maxByOrNull { it.column } ?: return 0
        return lastNote.column + advanceToColumns(lastNote.sourceSound.breakAfterBeats, stepBeats)
    }

    private fun rebuildSet(set: ScaleSet, notePositions: List<PositionedSound>, stepBeats: Float): ScaleSet {
        val ordered = notePositions.sortedWith(compareBy<PositionedSound> { it.column }.thenBy { it.originalOrder }.thenBy { it.midi })
        val rebuiltNotes = ordered.mapIndexed { index, note ->
            val nextColumn = ordered.getOrNull(index + 1)?.column
            val advanceBeats = if (nextColumn == null) {
                note.sourceSound.breakAfterBeats ?: DefaultAdvanceBeats
            } else {
                columnsToAdvance((nextColumn - note.column).coerceAtLeast(1), stepBeats)
            }
            note.sourceSound.copy(
                notes = note.sourceSound.notes,
                breakAfterBeats = advanceBeats,
            )
        }

        return set.copy(sounds = rebuiltNotes)
    }

    private fun rebuildTimelineSets(
        originalSets: List<ScaleSet>,
        positionedSounds: List<TimelinePositionedSound>,
        stepBeats: Float,
    ): List<ScaleSet> {
        val ordered = positionedSounds.sortedWith(
            compareBy<TimelinePositionedSound> { it.column }
                .thenBy { it.setIndex }
                .thenBy { it.originalOrder },
        )

        val rebuiltBySet = ordered.mapIndexed { index, sound ->
            val nextColumn = ordered.getOrNull(index + 1)?.column
            sound.copy(
                sourceSound = sound.sourceSound.copy(
                    breakAfterBeats = if (nextColumn == null) {
                        sound.sourceSound.breakAfterBeats ?: DefaultAdvanceBeats
                    } else {
                        columnsToAdvance((nextColumn - sound.column).coerceAtLeast(1), stepBeats)
                    },
                ),
            )
        }.groupBy { it.setIndex }

        return originalSets.indices.map { setIndex ->
            val sounds = rebuiltBySet[setIndex]
                ?.sortedWith(compareBy<TimelinePositionedSound> { it.column }.thenBy { it.originalOrder })
                ?.map { it.sourceSound }
                ?: emptyList()
            originalSets[setIndex].copy(sounds = sounds)
        }
    }

    private fun extractSoundPositions(set: ScaleSet, stepBeats: Float): List<PositionedSound> {
        var currentColumn = 0
        return buildList {
            set.sounds.forEachIndexed { index, sound ->
                if (sound.notes.isNotEmpty()) {
                    add(
                        PositionedSound(
                            soundId = sound.id,
                            sourceSound = sound,
                            midi = displayMidi(sound.notes),
                            column = currentColumn,
                            originalOrder = index,
                        ),
                    )
                    currentColumn += advanceToColumns(sound.breakAfterBeats, stepBeats)
                }
            }
        }
    }

    private fun extractTimelinePositions(sets: List<ScaleSet>, stepBeats: Float): List<TimelinePositionedSound> {
        val positioned = mutableListOf<TimelinePositionedSound>()
        var currentColumn = 0
        sets.forEachIndexed { setIndex, set ->
            val local = extractSoundPositions(set, stepBeats)
            local.forEach { sound ->
                positioned += TimelinePositionedSound(
                    soundId = sound.soundId,
                    setIndex = setIndex,
                    sourceSound = sound.sourceSound,
                    midi = sound.midi,
                    column = currentColumn + sound.column,
                    originalOrder = sound.originalOrder,
                )
            }
            currentColumn += nextFreeColumn(set, stepBeats)
        }
        return positioned
    }

    private fun advanceToColumns(breakAfterBeats: Float?, stepBeats: Float): Int {
        val beats = breakAfterBeats ?: DefaultAdvanceBeats
        return (beats / stepBeats).roundToInt().coerceAtLeast(1)
    }

    private fun displayMidi(notes: List<Int>): Int = notes.average().roundToInt()

    private fun transposeNotes(notes: List<Int>, previousDisplayMidi: Int, newDisplayMidi: Int): List<Int> {
        val delta = newDisplayMidi - previousDisplayMidi
        return if (notes.size == 1) {
            listOf(newDisplayMidi)
        } else {
            notes.map { it + delta }
        }
    }

    private fun columnsToAdvance(columns: Int, stepBeats: Float): Float {
        return columns * stepBeats
    }

    private data class PositionedSound(
        val soundId: String,
        val sourceSound: ScaleSound,
        val midi: Int,
        val column: Int,
        val originalOrder: Int,
    )

    private data class TimelinePositionedSound(
        val soundId: String,
        val setIndex: Int,
        val sourceSound: ScaleSound,
        val midi: Int,
        val column: Int,
        val originalOrder: Int,
    )
}
