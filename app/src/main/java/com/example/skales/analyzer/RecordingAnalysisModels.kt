package com.example.skales.analyzer

data class AudioRecording(
    val id: String,
    val filePath: String,
    val durationMs: Long,
    val sampleRateHz: Int,
    val channelCount: Int,
)

data class DetectedPitchFrame(
    val timeMs: Long,
    val frequencyHz: Float,
    val midi: Int,
    val confidence: Float,
    val amplitude: Float,
)

data class DetectedNoteEvent(
    val midi: Int,
    val startMs: Long,
    val endMs: Long,
    val confidence: Float,
    val sourceFrameCount: Int,
)

enum class PhraseShape {
    Ascending,
    Descending,
    AscendingThenDescending,
    DescendingThenAscending,
    Mixed,
}

data class DetectedPhrase(
    val noteEvents: List<DetectedNoteEvent>,
    val shape: PhraseShape,
)

data class NoteExtractionResult(
    val pitchFrames: List<DetectedPitchFrame>,
    val filteredPitchFrames: List<DetectedPitchFrame>,
    val noteEvents: List<DetectedNoteEvent>,
    val phrases: List<DetectedPhrase>,
)
