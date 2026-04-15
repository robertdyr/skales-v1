package com.example.skales.analyzer

interface Analyzer {
    suspend fun analyze(recording: AudioRecording): ScaleDraft?
}

class RecordingAnalysisPipeline(
    private val pitchDetector: PitchDetector,
    private val pitchFrameFilter: PitchFrameFilter = DefaultPitchFrameFilter(),
    private val noteEventReducer: NoteEventReducer = DefaultNoteEventReducer(),
    private val phraseSegmenter: PhraseSegmenter = DefaultPhraseSegmenter(),
    private val scaleCandidateRanker: ScaleCandidateRanker = DefaultScaleCandidateRanker(),
    private val scaleDraftBuilder: ScaleDraftBuilder = DefaultScaleDraftBuilder(),
) : Analyzer {
    override suspend fun analyze(recording: AudioRecording): ScaleDraft? {
        return analyzeDetailed(recording).suggestedScale
    }

    internal suspend fun analyzeDetailed(recording: AudioRecording): RecordingAnalysisResult {
        val pitchFrames = pitchDetector.detect(recording)
        val filteredPitchFrames = pitchFrameFilter.filter(pitchFrames)
        val noteEvents = noteEventReducer.reduce(filteredPitchFrames)
        val phrases = phraseSegmenter.segment(noteEvents)
        val candidates = scaleCandidateRanker.rank(phrases)
        val suggestedScale = scaleDraftBuilder.build(phrases, candidates)

        return RecordingAnalysisResult(
            pitchFrames = pitchFrames,
            filteredPitchFrames = filteredPitchFrames,
            noteEvents = noteEvents,
            phrases = phrases,
            candidates = candidates,
            suggestedScale = suggestedScale,
        )
    }
}
