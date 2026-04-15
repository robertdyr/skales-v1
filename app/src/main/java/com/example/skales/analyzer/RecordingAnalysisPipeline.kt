package com.example.skales.analyzer

interface Analyzer {
    suspend fun analyze(recording: AudioRecording): NoteExtractionResult
}

class RecordingAnalysisPipeline(
    private val pitchDetector: PitchDetector,
    private val pitchFrameFilter: PitchFrameFilter = DefaultPitchFrameFilter(),
    private val noteEventReducer: NoteEventReducer = DefaultNoteEventReducer(),
    private val phraseSegmenter: PhraseSegmenter = DefaultPhraseSegmenter(),
) : Analyzer {
    override suspend fun analyze(recording: AudioRecording): NoteExtractionResult {
        return analyzeDetailed(recording)
    }

    internal suspend fun analyzeDetailed(recording: AudioRecording): NoteExtractionResult {
        val pitchFrames = pitchDetector.detect(recording)
        val filteredPitchFrames = pitchFrameFilter.filter(pitchFrames)
        val noteEvents = noteEventReducer.reduce(filteredPitchFrames)
        val phrases = phraseSegmenter.segment(noteEvents)

        return NoteExtractionResult(
            pitchFrames = pitchFrames,
            filteredPitchFrames = filteredPitchFrames,
            noteEvents = noteEvents,
            phrases = phrases,
        )
    }
}
