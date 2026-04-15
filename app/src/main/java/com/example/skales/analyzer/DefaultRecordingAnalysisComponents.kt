package com.example.skales.analyzer

import com.example.skales.model.PlaybackTiming
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import kotlin.math.abs

interface NoiseGate {
    fun shouldKeep(frame: DetectedPitchFrame): Boolean
}

class AmplitudeNoiseGate(
    private val minAmplitude: Float = 0.01f,
) : NoiseGate {
    override fun shouldKeep(frame: DetectedPitchFrame): Boolean = frame.amplitude >= minAmplitude
}

interface PitchDetector {
    suspend fun detect(recording: AudioRecording): List<DetectedPitchFrame>
}

interface PitchFrameFilter {
    fun filter(frames: List<DetectedPitchFrame>): List<DetectedPitchFrame>
}

class DefaultPitchFrameFilter(
    private val minConfidence: Float = 0.6f,
    private val minAmplitude: Float = 0.01f,
    private val maxNeighborDistanceMs: Long = 120L,
) : PitchFrameFilter {
    override fun filter(frames: List<DetectedPitchFrame>): List<DetectedPitchFrame> {
        val kept = frames.filter { it.confidence >= minConfidence && it.amplitude >= minAmplitude }
        if (kept.size < 3) return kept

        return kept.filterIndexed { index, frame ->
            val previous = kept.getOrNull(index - 1)
            val next = kept.getOrNull(index + 1)
            if (previous == null || next == null) {
                true
            } else {
                val previousNear = frame.timeMs - previous.timeMs <= maxNeighborDistanceMs
                val nextNear = next.timeMs - frame.timeMs <= maxNeighborDistanceMs
                val isolatedOutlier = previousNear && nextNear &&
                    abs(previous.midi - frame.midi) >= 1 &&
                    abs(next.midi - frame.midi) >= 1 &&
                    previous.midi == next.midi
                !isolatedOutlier
            }
        }
    }
}

interface NoteEventReducer {
    fun reduce(frames: List<DetectedPitchFrame>): List<DetectedNoteEvent>
}

class DefaultNoteEventReducer(
    private val minEventDurationMs: Long = 120L,
) : NoteEventReducer {
    override fun reduce(frames: List<DetectedPitchFrame>): List<DetectedNoteEvent> {
        if (frames.isEmpty()) return emptyList()

        val sorted = frames.sortedBy { it.timeMs }
        val estimatedFrameDurationMs = sorted.zipWithNext { a, b -> b.timeMs - a.timeMs }
            .filter { it > 0L }
            .let { gaps -> if (gaps.isEmpty()) 50L else gaps.sum() / gaps.size }

        val events = mutableListOf<DetectedNoteEvent>()
        var groupStart = 0

        fun flush(endExclusive: Int) {
            val group = sorted.subList(groupStart, endExclusive)
            val start = group.first().timeMs
            val end = group.last().timeMs + estimatedFrameDurationMs
            if (end - start < minEventDurationMs) return
            events += DetectedNoteEvent(
                midi = group.first().midi,
                startMs = start,
                endMs = end,
                confidence = group.map { it.confidence }.average().toFloat(),
                sourceFrameCount = group.size,
            )
        }

        for (index in 1..sorted.lastIndex) {
            if (sorted[index].midi != sorted[groupStart].midi) {
                flush(index)
                groupStart = index
            }
        }
        flush(sorted.size)
        return events
    }
}

interface PhraseSegmenter {
    fun segment(noteEvents: List<DetectedNoteEvent>): List<DetectedPhrase>
}

class DefaultPhraseSegmenter(
    private val phraseGapMs: Long = 400L,
) : PhraseSegmenter {
    override fun segment(noteEvents: List<DetectedNoteEvent>): List<DetectedPhrase> {
        if (noteEvents.isEmpty()) return emptyList()

        val sorted = noteEvents.sortedBy { it.startMs }
        val phrases = mutableListOf<List<DetectedNoteEvent>>()
        var current = mutableListOf(sorted.first())

        for (index in 1..sorted.lastIndex) {
            val previous = sorted[index - 1]
            val next = sorted[index]
            if (next.startMs - previous.endMs > phraseGapMs) {
                phrases += current
                current = mutableListOf(next)
            } else {
                current += next
            }
        }
        phrases += current

        return phrases.map { events -> DetectedPhrase(noteEvents = events, shape = classifyShape(events)) }
    }

    private fun classifyShape(events: List<DetectedNoteEvent>): PhraseShape {
        val distinct = events.map { it.midi }.fold(mutableListOf<Int>()) { acc, midi ->
            if (acc.lastOrNull() != midi) acc += midi
            acc
        }
        if (distinct.size < 2) return PhraseShape.Mixed

        var sawUp = false
        var sawDown = false
        var lastDirection = 0
        var changedDirection = false

        distinct.zipWithNext().forEach { (left, right) ->
            val direction = when {
                right > left -> 1
                right < left -> -1
                else -> 0
            }
            if (direction == 1) sawUp = true
            if (direction == -1) sawDown = true
            if (direction != 0 && lastDirection != 0 && direction != lastDirection) changedDirection = true
            if (direction != 0) lastDirection = direction
        }

        return when {
            sawUp && !sawDown -> PhraseShape.Ascending
            sawDown && !sawUp -> PhraseShape.Descending
            sawUp && sawDown && changedDirection && distinct.last() <= distinct.first() -> PhraseShape.AscendingThenDescending
            sawUp && sawDown && changedDirection && distinct.last() >= distinct.first() -> PhraseShape.DescendingThenAscending
            else -> PhraseShape.Mixed
        }
    }
}

interface ScaleCandidateRanker {
    fun rank(phrases: List<DetectedPhrase>): List<ScaleCandidate>
}

class DefaultScaleCandidateRanker : ScaleCandidateRanker {
    override fun rank(phrases: List<DetectedPhrase>): List<ScaleCandidate> {
        val pitchClasses = phrases
            .flatMap { it.noteEvents }
            .map { ((it.midi % 12) + 12) % 12 }
            .distinct()

        if (pitchClasses.isEmpty()) return emptyList()

        return scaleTypes.flatMap { scaleType ->
            (0..11).map { root ->
                val expected = scaleType.intervals.map { (root + it) % 12 }.toSet()
                val matches = pitchClasses.count { it in expected }
                val extras = pitchClasses.count { it !in expected }
                val confidence = (matches.toFloat() / pitchClasses.size) - (extras.toFloat() / (pitchClasses.size * 2f))
                ScaleCandidate(
                    rootPitchClass = root,
                    scaleType = scaleType.name,
                    confidence = confidence,
                    reasons = listOf("matched $matches/${pitchClasses.size} pitch classes"),
                )
            }
        }.sortedByDescending { it.confidence }
    }

    private data class ScaleTypeDefinition(
        val name: String,
        val intervals: List<Int>,
    )

    private companion object {
        val scaleTypes = listOf(
            ScaleTypeDefinition("major pentatonic", listOf(0, 2, 4, 7, 9)),
            ScaleTypeDefinition("major", listOf(0, 2, 4, 5, 7, 9, 11)),
            ScaleTypeDefinition("natural minor", listOf(0, 2, 3, 5, 7, 8, 10)),
        )
    }
}

interface ScaleDraftBuilder {
    fun build(phrases: List<DetectedPhrase>, candidates: List<ScaleCandidate>): ScaleDraft?
}

class DefaultScaleDraftBuilder(
    private val defaultBpm: Int = 92,
) : ScaleDraftBuilder {
    override fun build(phrases: List<DetectedPhrase>, candidates: List<ScaleCandidate>): ScaleDraft? {
        if (phrases.isEmpty()) return null

        val topCandidate = candidates.firstOrNull()
        val nameSuggestion = if (topCandidate == null) {
            "Imported scale"
        } else {
            "${pitchClassName(topCandidate.rootPitchClass)} ${topCandidate.scaleType}"
        }

        return ScaleDraft(
            nameSuggestion = nameSuggestion,
            sets = phrases.map { phrase ->
                ScaleSet(
                    sounds = phrase.noteEvents.map { event ->
                        ScaleSound(
                            notes = listOf(event.midi),
                            kind = ScaleSoundKind.Note,
                        )
                    },
                )
            },
            timing = PlaybackTiming(defaultBpm = defaultBpm),
        )
    }

    private fun pitchClassName(rootPitchClass: Int): String {
        return listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")[rootPitchClass]
    }
}
