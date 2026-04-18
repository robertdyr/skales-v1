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
    const val InternalStepBeats = FineStepBeats
    const val DefaultMinMidi = 21
    const val DefaultMaxMidi = 108
    const val DefaultAdvanceSteps = 4
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
        val normalizedSets = normalizeSets(sets)
        val layout = computeLayout(normalizedSets)
        val notes = normalizedSets.flatMapIndexed { setIndex, set ->
            set.sounds
                .filter { it.notes.isNotEmpty() }
                .map { sound ->
                    SetGridNote(
                        soundId = sound.id,
                        midis = sound.notes.map { midi -> midi.coerceIn(minMidi, maxMidi) },
                        column = sound.step.coerceAtLeast(0),
                        setIndex = setIndex,
                        kind = sound.kind,
                    )
                }
        }
        val farthestColumn = maxOf(
            notes.maxOfOrNull { it.column + 1 } ?: 0,
            layout.timelineEndExclusive,
        )

        return SetGrid(
            notes = notes,
            columnCount = maxOf(MinimumColumnCount, farthestColumn + 4),
            minMidi = minMidi,
            maxMidi = maxMidi,
            stepBeats = InternalStepBeats,
            setStartColumns = layout.setStartColumns,
        )
    }

    fun nextFreeColumn(
        sets: List<ScaleSet>,
        selectedSetIndex: Int,
        snapStepBeats: Float = DefaultStepBeats,
    ): Int {
        val normalizedSets = normalizeSets(sets)
        val safeIndex = selectedSetIndex.coerceIn(0, normalizedSets.lastIndex)
        val layout = computeLayout(normalizedSets)
        val setStart = layout.setStartColumns[safeIndex]
        val nextStart = layout.nextSetStartColumn(safeIndex)
        val lastStepInSet = normalizedSets[safeIndex].sounds.maxOfOrNull { it.step }
        val targetGlobalColumn = if (lastStepInSet == null) {
            setStart
        } else {
            lastStepInSet + snapStepSize(snapStepBeats)
        }
        return (targetGlobalColumn.coerceAtMost(nextStart - 1) - setStart).coerceAtLeast(0)
    }

    fun addSoundInTimeline(
        sets: List<ScaleSet>,
        selectedSetIndex: Int,
        midi: Int,
        localColumn: Int,
        kind: ScaleSoundKind = ScaleSoundKind.Note,
        snapStepBeats: Float = DefaultStepBeats,
    ): List<ScaleSet> {
        val normalizedSets = normalizeSets(sets)
        val safeIndex = selectedSetIndex.coerceIn(0, normalizedSets.lastIndex)
        val layout = computeLayout(normalizedSets)
        val targetStep = quantizeForSet(
            globalColumn = layout.setStartColumns[safeIndex] + localColumn.coerceAtLeast(0),
            selectedSetIndex = safeIndex,
            layout = layout,
            snapStepBeats = snapStepBeats,
        )

        return normalizedSets.toMutableList().apply {
            this[safeIndex] = this[safeIndex].copy(
                sounds = (this[safeIndex].sounds + ScaleSound(
                    notes = listOf(midi),
                    kind = kind,
                    step = targetStep,
                )).sortedBy { it.step },
            )
        }
    }

    fun moveSoundInTimeline(
        sets: List<ScaleSet>,
        selectedSetIndex: Int,
        soundId: String,
        midi: Int,
        globalColumn: Int,
        snapStepBeats: Float = DefaultStepBeats,
    ): List<ScaleSet> {
        val normalizedSets = normalizeSets(sets)
        val safeIndex = selectedSetIndex.coerceIn(0, normalizedSets.lastIndex)
        val layout = computeLayout(normalizedSets)
        val set = normalizedSets[safeIndex]
        val target = set.sounds.firstOrNull { it.id == soundId } ?: return normalizedSets
        val orderedSetSounds = set.sounds.sortedBy { it.step }
        val setStart = layout.setStartColumns[safeIndex]
        val nextSetStart = layout.nextSetStartColumn(safeIndex)
        val firstSoundId = orderedSetSounds.firstOrNull()?.id
        val movesBoundary = safeIndex > 0 && target.id == firstSoundId
        val minAllowed = if (movesBoundary) {
            layout.previousSetMaxOccupiedStep(safeIndex) + 1
        } else {
            setStart
        }
        val quantizedTarget = quantizeColumn(globalColumn, snapStepBeats)
        val clampedTarget = if (movesBoundary) {
            quantizedTarget.coerceAtLeast(minAllowed)
        } else {
            quantizedTarget.coerceIn(minAllowed, nextSetStart - 1)
        }
        val crossedBoundary = movesBoundary && safeIndex < normalizedSets.lastIndex && clampedTarget >= nextSetStart
        val boundaryShift = if (crossedBoundary) (clampedTarget + 1) - nextSetStart else 0

        return normalizedSets.mapIndexed { setIndex, currentSet ->
            when {
                setIndex == safeIndex -> {
                    currentSet.copy(
                        sounds = currentSet.sounds.map { sound ->
                            if (sound.id == soundId) {
                                sound.copy(
                                    notes = transposeNotes(sound.notes, displayMidi(sound.notes), midi),
                                    step = clampedTarget,
                                )
                            } else {
                                sound
                            }
                        }.sortedBy { it.step },
                    )
                }
                crossedBoundary && setIndex > safeIndex -> {
                    currentSet.copy(sounds = currentSet.sounds.map { sound -> sound.copy(step = sound.step + boundaryShift) })
                }
                else -> currentSet
            }
        }
    }

    fun moveSetBoundaryInTimeline(
        sets: List<ScaleSet>,
        targetSetIndex: Int,
        globalColumn: Int,
        snapStepBeats: Float = DefaultStepBeats,
    ): List<ScaleSet> {
        val normalizedSets = normalizeSets(sets)
        if (targetSetIndex <= 0 || targetSetIndex > normalizedSets.lastIndex) return normalizedSets

        val layout = computeLayout(normalizedSets)
        val currentSetStart = layout.setStartColumns[targetSetIndex]
        val targetStart = quantizeColumn(globalColumn, snapStepBeats)
            .coerceAtLeast(layout.previousSetMaxOccupiedStep(targetSetIndex) + 1)
        val delta = targetStart - currentSetStart
        if (delta == 0) return normalizedSets

        return normalizedSets.mapIndexed { setIndex, set ->
            if (setIndex >= targetSetIndex) {
                set.copy(sounds = set.sounds.map { sound -> sound.copy(step = sound.step + delta) })
            } else {
                set
            }
        }
    }

    fun removeNote(set: ScaleSet, soundId: String): ScaleSet {
        return set.copy(sounds = set.sounds.filterNot { it.id == soundId })
    }

    private fun normalizeSets(sets: List<ScaleSet>): List<ScaleSet> {
        return if (sets.isEmpty()) listOf(ScaleSet(sounds = emptyList())) else sets
    }

    private fun computeLayout(sets: List<ScaleSet>): TimelineLayout {
        val setStarts = mutableListOf<Int>()
        val maxOccupiedSteps = mutableListOf<Int>()
        var nextAvailableStart = 0

        sets.forEachIndexed { setIndex, set ->
            val playableSounds = set.sounds.filter { it.notes.isNotEmpty() }.sortedBy { it.step }
            val setStart = when {
                setIndex == 0 -> 0
                else -> playableSounds.firstOrNull()?.step ?: nextAvailableStart
            }
            val maxOccupiedStep = playableSounds.maxOfOrNull { it.step } ?: (setStart - 1)
            val setEndExclusive = if (maxOccupiedStep >= setStart) {
                maxOccupiedStep + DefaultAdvanceSteps
            } else {
                setStart + DefaultAdvanceSteps
            }
            setStarts += setStart
            maxOccupiedSteps += maxOccupiedStep
            nextAvailableStart = maxOf(nextAvailableStart, setEndExclusive)
        }

        return TimelineLayout(
            setStartColumns = setStarts.ifEmpty { listOf(0) },
            setMaxOccupiedColumns = maxOccupiedSteps.ifEmpty { listOf(-1) },
            timelineEndExclusive = nextAvailableStart,
        )
    }

    private fun quantizeForSet(
        globalColumn: Int,
        selectedSetIndex: Int,
        layout: TimelineLayout,
        snapStepBeats: Float,
    ): Int {
        val setStart = layout.setStartColumns[selectedSetIndex]
        val nextStart = layout.nextSetStartColumn(selectedSetIndex)
        return quantizeColumn(globalColumn, snapStepBeats)
            .coerceIn(setStart, nextStart - 1)
    }

    private fun quantizeColumn(column: Int, snapStepBeats: Float): Int {
        val snapSize = snapStepSize(snapStepBeats)
        return ((column.toFloat() / snapSize).roundToInt() * snapSize).coerceAtLeast(0)
    }

    private fun snapStepSize(snapStepBeats: Float): Int {
        return (snapStepBeats / InternalStepBeats).roundToInt().coerceAtLeast(1)
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

    private data class TimelineLayout(
        val setStartColumns: List<Int>,
        val setMaxOccupiedColumns: List<Int>,
        val timelineEndExclusive: Int,
    ) {
        fun nextSetStartColumn(setIndex: Int): Int {
            return setStartColumns.getOrNull(setIndex + 1) ?: Int.MAX_VALUE
        }

        fun previousSetMaxOccupiedStep(setIndex: Int): Int {
            return setMaxOccupiedColumns.getOrElse(setIndex - 1) { -1 }
        }
    }
}
