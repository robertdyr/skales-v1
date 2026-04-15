package com.example.skales.model

data class Note(
    val name: String,
    val midiNumber: Int,
    val octave: Int,
    val isBlackKey: Boolean,
) {
    companion object {
        private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        fun fromMidi(midiNumber: Int): Note {
            val noteIndex = midiNumber.mod(12)
            val name = noteNames[noteIndex]
            return Note(
                name = name,
                midiNumber = midiNumber,
                octave = (midiNumber / 12) - 1,
                isBlackKey = name.contains('#'),
            )
        }
    }
}
