package com.example.skales.analyzer

import com.example.skales.infer.DefaultScaleCandidateRanker
import com.example.skales.infer.DefaultScaleDraftBuilder
import com.example.skales.infer.DefaultScaleInferEngine
import com.example.skales.infer.ScaleCandidate
import com.example.skales.infer.ScaleInferenceRequest
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRecordingAnalysisComponentsTest {
    @Test
    fun pitchFrameFilter_removesLowConfidenceAndIsolatedOutlierFrames() {
        val filter = DefaultPitchFrameFilter(minConfidence = 0.6f, minAmplitude = 0.01f)

        val filtered = filter.filter(
            listOf(
                frame(timeMs = 0, midi = 60),
                frame(timeMs = 50, midi = 60, confidence = 0.3f),
                frame(timeMs = 100, midi = 61),
                frame(timeMs = 150, midi = 60),
                frame(timeMs = 200, midi = 60),
            ),
        )

        assertEquals(listOf(60, 60, 60), filtered.map { it.midi })
    }

    @Test
    fun noteEventReducer_mergesStableFramesAndDropsTinyBlips() {
        val reducer = DefaultNoteEventReducer(minEventDurationMs = 120L)

        val events = reducer.reduce(
            listOf(
                frame(timeMs = 0, midi = 60),
                frame(timeMs = 50, midi = 60),
                frame(timeMs = 100, midi = 60),
                frame(timeMs = 150, midi = 61),
                frame(timeMs = 250, midi = 62),
                frame(timeMs = 300, midi = 62),
                frame(timeMs = 350, midi = 62),
            ),
        )

        assertEquals(listOf(60, 62), events.map { it.midi })
        assertTrue(events.all { it.endMs > it.startMs })
    }

    @Test
    fun phraseSegmenter_keepsUpDownRunInOnePhraseAndClassifiesShape() {
        val segmenter = DefaultPhraseSegmenter(phraseGapMs = 400L)

        val phrases = segmenter.segment(
            listOf(
                event(midi = 60, startMs = 0, endMs = 200),
                event(midi = 62, startMs = 220, endMs = 420),
                event(midi = 64, startMs = 440, endMs = 640),
                event(midi = 62, startMs = 660, endMs = 860),
                event(midi = 60, startMs = 880, endMs = 1080),
            ),
        )

        assertEquals(1, phrases.size)
        assertEquals(PhraseShape.AscendingThenDescending, phrases.single().shape)
    }

    @Test
    fun scaleCandidateRanker_prefersMajorPentatonicForMatchingPitchClasses() {
        val ranker = DefaultScaleCandidateRanker()

        val candidates = ranker.rankFromPhrases(
            listOf(
                DetectedPhrase(
                    noteEvents = listOf(
                        event(midi = 60),
                        event(midi = 62),
                        event(midi = 64),
                        event(midi = 67),
                        event(midi = 69),
                        event(midi = 67),
                        event(midi = 64),
                        event(midi = 62),
                        event(midi = 60),
                    ),
                    shape = PhraseShape.AscendingThenDescending,
                ),
            ),
        )

        assertFalse(candidates.isEmpty())
        assertEquals("major pentatonic", candidates.first().scaleType)
        assertEquals(0, candidates.first().rootPitchClass)
    }

    @Test
    fun scaleDraftBuilder_buildsPlaybackSetsFromPhrases() {
        val builder = DefaultScaleDraftBuilder()

        val draft = builder.buildFromPhrases(
            phrases = listOf(
                DetectedPhrase(
                    noteEvents = listOf(event(midi = 60), event(midi = 62), event(midi = 64)),
                    shape = PhraseShape.Ascending,
                ),
            ),
            candidates = listOf(
                ScaleCandidate(
                    rootPitchClass = 0,
                    scaleType = "major pentatonic",
                    confidence = 0.9f,
                    reasons = emptyList(),
                ),
            ),
        )

        assertNotNull(draft)
        assertEquals("C major pentatonic", draft?.nameSuggestion)
        assertEquals(listOf(60, 62, 64), draft?.sets?.single()?.sounds?.map { it.notes.single() })
    }

    @Test
    fun scaleInferEngine_preservesLockedSetsWhenReinferring() {
        val inferEngine = DefaultScaleInferEngine()

        val result = inferEngine.infer(
            ScaleInferenceRequest(
                currentSets = listOf(
                    ScaleSet(sounds = listOf(ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note))),
                    ScaleSet(sounds = listOf(ScaleSound(notes = listOf(62), kind = ScaleSoundKind.Note))),
                ),
                lockedSetIndices = setOf(0, 1),
                setCount = 7,
            ),
        )

        assertNotNull(result.suggestedScale)
        assertEquals(listOf(60), result.suggestedScale?.sets?.get(0)?.sounds?.single()?.notes)
        assertEquals(listOf(62), result.suggestedScale?.sets?.get(1)?.sounds?.single()?.notes)
        assertEquals(7, result.suggestedScale?.sets?.size)
    }

    @Test
    fun recordingAnalysisPipeline_runsStagesInOrder() = runBlocking {
        val pipeline = RecordingAnalysisPipeline(
            pitchDetector = object : PitchDetector {
                override suspend fun detect(recording: AudioRecording): List<DetectedPitchFrame> {
                    return listOf(
                        frame(timeMs = 0, midi = 60),
                        frame(timeMs = 50, midi = 60),
                        frame(timeMs = 100, midi = 60),
                        frame(timeMs = 150, midi = 62),
                        frame(timeMs = 200, midi = 62),
                        frame(timeMs = 250, midi = 62),
                        frame(timeMs = 300, midi = 64),
                        frame(timeMs = 350, midi = 64),
                        frame(timeMs = 400, midi = 64),
                    )
                }
            },
            pitchFrameFilter = DefaultPitchFrameFilter(),
            noteEventReducer = DefaultNoteEventReducer(),
            phraseSegmenter = DefaultPhraseSegmenter(),
        )

        val result = pipeline.analyzeDetailed(
            AudioRecording(
                id = "rec-1",
                filePath = "test.wav",
                durationMs = 1000,
                sampleRateHz = 44_100,
                channelCount = 1,
            ),
        )

        assertEquals(9, result.pitchFrames.size)
        assertEquals(listOf(60, 62, 64), result.noteEvents.map { it.midi })
        assertEquals(1, result.phrases.size)
    }

    private fun frame(
        timeMs: Long,
        midi: Int,
        confidence: Float = 0.95f,
        amplitude: Float = 0.5f,
    ): DetectedPitchFrame {
        return DetectedPitchFrame(
            timeMs = timeMs,
            frequencyHz = 440f,
            midi = midi,
            confidence = confidence,
            amplitude = amplitude,
        )
    }

    private fun event(
        midi: Int,
        startMs: Long = 0L,
        endMs: Long = startMs + 200L,
        confidence: Float = 0.95f,
        sourceFrameCount: Int = 4,
    ): DetectedNoteEvent {
        return DetectedNoteEvent(
            midi = midi,
            startMs = startMs,
            endMs = endMs,
            confidence = confidence,
            sourceFrameCount = sourceFrameCount,
        )
    }
}
