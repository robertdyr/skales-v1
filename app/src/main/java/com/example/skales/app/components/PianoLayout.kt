package com.example.skales.app.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.skales.model.Note

data class PianoLayoutKey(
    val midi: Int,
    val note: Note,
    val left: Dp,
    val width: Dp,
) {
    val center: Dp
        get() = left + (width / 2)
}

object PianoLayout {
    const val StartMidi = 21
    const val EndMidi = 108
    val WhiteKeyWidth = 44.dp
    val BlackKeyWidth = 28.dp

    fun keys(): List<PianoLayoutKey> {
        val allKeys = (StartMidi..EndMidi).map { midi -> midi to Note.fromMidi(midi) }
        val whiteMidis = allKeys.filterNot { it.second.isBlackKey }.map { it.first }
        val whiteIndexByMidi = whiteMidis.withIndex().associate { it.value to it.index }

        return allKeys.map { (midi, note) ->
            if (note.isBlackKey) {
                val previousWhiteIndex = whiteMidis.indexOfLast { it < midi }
                val base = WhiteKeyWidth * (previousWhiteIndex + 1)
                val centerOffset = (WhiteKeyWidth - BlackKeyWidth) / 2
                PianoLayoutKey(
                    midi = midi,
                    note = note,
                    left = base - centerOffset,
                    width = BlackKeyWidth,
                )
            } else {
                PianoLayoutKey(
                    midi = midi,
                    note = note,
                    left = WhiteKeyWidth * (whiteIndexByMidi.getValue(midi)),
                    width = WhiteKeyWidth,
                )
            }
        }
    }

    fun totalWidth(keys: List<PianoLayoutKey> = keys()): Dp {
        val whiteKeyCount = keys.count { !it.note.isBlackKey }
        return WhiteKeyWidth * whiteKeyCount
    }
}
