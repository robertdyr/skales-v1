package com.example.skales.analyzer

import com.example.skales.model.PlaybackTiming
import com.example.skales.model.ScaleSet

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

data class ScaleCandidate(
    val rootPitchClass: Int,
    val scaleType: String,
    val confidence: Float,
    val reasons: List<String>,
)

data class ScaleDraft(
    val nameSuggestion: String,
    val sets: List<ScaleSet>,
    val timing: PlaybackTiming,
)

data class RecordingAnalysisResult(
    val pitchFrames: List<DetectedPitchFrame>,
    val filteredPitchFrames: List<DetectedPitchFrame>,
    val noteEvents: List<DetectedNoteEvent>,
    val phrases: List<DetectedPhrase>,
    val candidates: List<ScaleCandidate>,
    val suggestedScale: ScaleDraft?,
)
