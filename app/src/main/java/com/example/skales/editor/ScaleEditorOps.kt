package com.example.skales.editor

import com.example.skales.model.Note
import com.example.skales.model.PlaybackTiming
import com.example.skales.model.Scale
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind

object ScaleEditorOps {
    fun defaultSets(): List<ScaleSet> = listOf(ScaleSet(sounds = emptyList()))

    fun toSingleWorkingSet(sets: List<ScaleSet>): List<ScaleSet> {
        val flattenedSounds = sets.flatMap { it.sounds }
        return listOf(ScaleSet(sounds = flattenedSounds))
    }

    fun normalizeSets(sets: List<ScaleSet>): List<ScaleSet> {
        return if (sets.isEmpty()) defaultSets() else sets
    }

    fun normalizeSelectedSetIndex(sets: List<ScaleSet>, selectedSetIndex: Int): Int {
        return selectedSetIndex.coerceIn(0, normalizeSets(sets).lastIndex)
    }

    fun addSet(sets: List<ScaleSet>): Pair<List<ScaleSet>, Int> {
        val nextSets = normalizeSets(sets) + ScaleSet(sounds = emptyList())
        return nextSets to nextSets.lastIndex
    }

    fun deleteSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int): Pair<List<ScaleSet>, Int> {
        val normalizedSets = normalizeSets(sets)
        if (normalizedSets.size == 1) return defaultSets() to 0

        val safeIndex = normalizeSelectedSetIndex(normalizedSets, selectedSetIndex)
        val nextSets = normalizedSets.toMutableList().apply { removeAt(safeIndex) }
        return nextSets to safeIndex.coerceAtMost(nextSets.lastIndex)
    }

    fun addChordPreCueToSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            val chordNotes = chordCueNotes(set)
            if (chordNotes.isEmpty()) {
                set
            } else {
                val cue = ScaleSound(notes = chordNotes, kind = ScaleSoundKind.Cue)
                val updatedSounds = if (set.sounds.firstOrNull()?.kind == ScaleSoundKind.Cue) {
                    listOf(cue) + set.sounds.drop(1)
                } else {
                    listOf(cue) + set.sounds
                }
                set.copy(sounds = updatedSounds)
            }
        }
    }

    fun addChordPostCueToSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            val chordNotes = chordCueNotes(set)
            if (chordNotes.isEmpty()) {
                set
            } else {
                val cue = ScaleSound(notes = chordNotes, kind = ScaleSoundKind.Cue)
                val updatedSounds = if (set.sounds.lastOrNull()?.kind == ScaleSoundKind.Cue) {
                    set.sounds.dropLast(1) + cue
                } else {
                    set.sounds + cue
                }
                set.copy(sounds = updatedSounds)
            }
        }
    }

    fun removeChordPreCueFromSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            set.copy(sounds = set.sounds.filterIndexed { index, sound ->
                !(index == 0 && sound.kind == ScaleSoundKind.Cue)
            })
        }
    }

    fun removeChordPostCueFromSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            val sounds = set.sounds
            if (sounds.lastOrNull()?.kind == ScaleSoundKind.Cue) {
                set.copy(sounds = sounds.dropLast(1))
            } else {
                set
            }
        }
    }

    fun addNoteToSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int, midi: Int): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            set.copy(
                sounds = set.sounds + ScaleSound(
                    notes = listOf(midi),
                    kind = ScaleSoundKind.Note,
                ),
            )
        }
    }

    fun addNoteToSelectedSetAtColumn(
        sets: List<ScaleSet>,
        selectedSetIndex: Int,
        midi: Int,
        column: Int,
        stepBeats: Float,
    ): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            SetGridOps.addNoteAtColumn(set, midi, column, stepBeats)
        }
    }

    fun moveNoteInSelectedSet(
        sets: List<ScaleSet>,
        selectedSetIndex: Int,
        soundId: String,
        midi: Int,
        column: Int,
        stepBeats: Float,
    ): List<ScaleSet> {
        return SetGridOps.moveSoundInTimeline(sets, selectedSetIndex, soundId, midi, column, stepBeats)
    }

    fun moveSetBoundary(
        sets: List<ScaleSet>,
        targetSetIndex: Int,
        column: Int,
        stepBeats: Float,
    ): List<ScaleSet> {
        return SetGridOps.moveSetBoundaryInTimeline(sets, targetSetIndex, column, stepBeats)
    }

    fun removeNoteFromSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int, soundId: String, stepBeats: Float): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            SetGridOps.removeNote(set, soundId, stepBeats)
        }
    }

    fun removeLastSoundFromSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set ->
            if (set.sounds.isEmpty()) set else set.copy(sounds = set.sounds.dropLast(1))
        }
    }

    fun clearSelectedSet(sets: List<ScaleSet>, selectedSetIndex: Int): List<ScaleSet> {
        return mutateSelectedSet(sets, selectedSetIndex) { set -> set.copy(sounds = emptyList()) }
    }

    fun buildDraftScale(scaleId: String?, name: String, sets: List<ScaleSet>, bpm: Int): Scale? {
        val playableSets = sets
            .map { set -> set.copy(sounds = set.sounds.filter { sound -> sound.notes.isNotEmpty() }) }
            .filter { it.sounds.isNotEmpty() }
        if (playableSets.isEmpty()) return null

        return Scale(
            id = scaleId.orEmpty(),
            name = name,
            sets = playableSets,
            timing = PlaybackTiming(defaultBpm = bpm),
        )
    }

    fun buildSavableScale(scaleId: String?, name: String, sets: List<ScaleSet>, bpm: Int): Scale? {
        val draft = buildDraftScale(scaleId, name, sets, bpm) ?: return null
        if (draft.name.isBlank()) return null
        return draft.copy(name = draft.name.trim())
    }

    fun labelForSound(sound: ScaleSound): String {
        return sound.notes.joinToString(separator = " + ") { midi ->
            val note = Note.fromMidi(midi)
            "${note.name}${note.octave}"
        }
    }

    private fun mutateSelectedSet(
        sets: List<ScaleSet>,
        selectedSetIndex: Int,
        transform: (ScaleSet) -> ScaleSet,
    ): List<ScaleSet> {
        val normalizedSets = normalizeSets(sets)
        val safeIndex = normalizeSelectedSetIndex(normalizedSets, selectedSetIndex)
        return normalizedSets.toMutableList().apply {
            this[safeIndex] = transform(this[safeIndex])
        }
    }

    private fun chordCueNotes(set: ScaleSet): List<Int> {
        return set.sounds
            .filter { it.kind == ScaleSoundKind.Note }
            .flatMap { it.notes }
            .distinct()
            .take(3)
    }
}
