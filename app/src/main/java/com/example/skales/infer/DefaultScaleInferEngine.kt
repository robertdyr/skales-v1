package com.example.skales.infer

import com.example.skales.analyzer.DetectedPhrase
import com.example.skales.editor.SetGridOps
import com.example.skales.model.PlaybackTiming
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind

interface ScaleCandidateRanker {
    fun rankFromPitchClasses(pitchClasses: Set<Int>): List<ScaleCandidate>

    fun rankFromPhrases(phrases: List<DetectedPhrase>): List<ScaleCandidate> {
        val pitchClasses = phrases
            .flatMap { it.noteEvents }
            .map { ((it.midi % 12) + 12) % 12 }
            .toSet()
        return rankFromPitchClasses(pitchClasses)
    }
}

class DefaultScaleCandidateRanker : ScaleCandidateRanker {
    override fun rankFromPitchClasses(pitchClasses: Set<Int>): List<ScaleCandidate> {
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

    internal data class ScaleTypeDefinition(
        val name: String,
        val intervals: List<Int>,
    )

    internal companion object {
        val scaleTypes = listOf(
            ScaleTypeDefinition("major pentatonic", listOf(0, 2, 4, 7, 9)),
            ScaleTypeDefinition("major", listOf(0, 2, 4, 5, 7, 9, 11)),
            ScaleTypeDefinition("natural minor", listOf(0, 2, 3, 5, 7, 8, 10)),
        )
    }
}

interface ScaleDraftBuilder {
    fun buildFromPhrases(phrases: List<DetectedPhrase>, candidates: List<ScaleCandidate>, defaultBpm: Int = 92): ScaleDraft?

    fun buildFromInference(request: ScaleInferenceRequest, candidates: List<ScaleCandidate>): ScaleDraft?
}

class DefaultScaleDraftBuilder : ScaleDraftBuilder {
    override fun buildFromPhrases(
        phrases: List<DetectedPhrase>,
        candidates: List<ScaleCandidate>,
        defaultBpm: Int,
    ): ScaleDraft? {
        if (phrases.isEmpty()) return null

        val topCandidate = candidates.firstOrNull()
        val nameSuggestion = topCandidate?.let { "${pitchClassName(it.rootPitchClass)} ${it.scaleType}" } ?: "Imported scale"

        return ScaleDraft(
            nameSuggestion = nameSuggestion,
            sets = phrases.mapIndexed { phraseIndex, phrase ->
                val setStartStep = phraseIndex * SetGridOps.DefaultAdvanceSteps
                ScaleSet(
                    sounds = phrase.noteEvents.mapIndexed { eventIndex, event ->
                        ScaleSound(
                            notes = listOf(event.midi),
                            kind = ScaleSoundKind.Note,
                            step = setStartStep + (eventIndex * SetGridOps.DefaultAdvanceSteps),
                        )
                    },
                )
            },
            timing = PlaybackTiming(defaultBpm = defaultBpm),
        )
    }

    override fun buildFromInference(request: ScaleInferenceRequest, candidates: List<ScaleCandidate>): ScaleDraft? {
        val topCandidate = candidates.firstOrNull() ?: return null
        val scaleType = DefaultScaleCandidateRanker.scaleTypes.firstOrNull { it.name == topCandidate.scaleType } ?: return null
        val targetSetCount = request.setCount.coerceAtLeast(request.currentSets.size).coerceAtLeast(scaleType.intervals.size)
        val seededMidis = request.currentSets
            .flatMap { set -> set.sounds }
            .flatMap { sound -> sound.notes }
        val baseMidi = seededMidis.minOrNull()?.let { anchorRootMidi(it, topCandidate.rootPitchClass) } ?: (60 + topCandidate.rootPitchClass)
        val inferredSets = buildInferredSets(scaleType.intervals, baseMidi, targetSetCount)
        val mergedSets = MutableList(targetSetCount) { index ->
            when {
                index in request.lockedSetIndices && index < request.currentSets.size -> request.currentSets[index]
                index < inferredSets.size -> inferredSets[index]
                else -> ScaleSet(sounds = emptyList())
            }
        }

        return ScaleDraft(
            nameSuggestion = request.nameHint?.takeIf { it.isNotBlank() } ?: "${pitchClassName(topCandidate.rootPitchClass)} ${topCandidate.scaleType}",
            sets = mergedSets,
            timing = PlaybackTiming(defaultBpm = request.defaultBpm),
        )
    }

    private fun buildInferredSets(intervals: List<Int>, baseMidi: Int, targetSetCount: Int): List<ScaleSet> {
        return (0 until targetSetCount).map { index ->
            val octaveOffset = (index / intervals.size) * 12
            val midi = baseMidi + intervals[index % intervals.size] + octaveOffset
            ScaleSet(
                sounds = listOf(
                    ScaleSound(
                        notes = listOf(midi),
                        kind = ScaleSoundKind.Note,
                        step = index * SetGridOps.DefaultAdvanceSteps,
                    ),
                ),
            )
        }
    }

    private fun anchorRootMidi(observedMidi: Int, rootPitchClass: Int): Int {
        var candidate = observedMidi - (((observedMidi % 12) + 12) % 12 - rootPitchClass)
        while (((candidate % 12) + 12) % 12 != rootPitchClass) {
            candidate -= 1
        }
        while (candidate > observedMidi) {
            candidate -= 12
        }
        return candidate
    }

    private fun pitchClassName(rootPitchClass: Int): String {
        return listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")[rootPitchClass]
    }
}

interface ScaleInferEngine {
    fun infer(request: ScaleInferenceRequest): ScaleInferenceResult

    fun inferFromPhrases(phrases: List<DetectedPhrase>, defaultBpm: Int = 92): ScaleInferenceResult
}

class DefaultScaleInferEngine(
    private val candidateRanker: ScaleCandidateRanker = DefaultScaleCandidateRanker(),
    private val draftBuilder: ScaleDraftBuilder = DefaultScaleDraftBuilder(),
) : ScaleInferEngine {
    override fun infer(request: ScaleInferenceRequest): ScaleInferenceResult {
        val pitchClasses = request.currentSets
            .flatMap { set -> set.sounds }
            .flatMap { sound -> sound.notes }
            .map { ((it % 12) + 12) % 12 }
            .toSet()
        val candidates = candidateRanker.rankFromPitchClasses(pitchClasses)
        return ScaleInferenceResult(
            candidates = candidates,
            suggestedScale = draftBuilder.buildFromInference(request, candidates),
        )
    }

    override fun inferFromPhrases(phrases: List<DetectedPhrase>, defaultBpm: Int): ScaleInferenceResult {
        val candidates = candidateRanker.rankFromPhrases(phrases)
        return ScaleInferenceResult(
            candidates = candidates,
            suggestedScale = draftBuilder.buildFromPhrases(phrases, candidates, defaultBpm),
        )
    }
}
