package com.example.skales.infer

import com.example.skales.model.PlaybackTiming
import com.example.skales.model.ScaleSet

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

data class ScaleInferenceRequest(
    val currentSets: List<ScaleSet>,
    val lockedSetIndices: Set<Int> = currentSets.indices
        .filter { index -> currentSets[index].sounds.isNotEmpty() }
        .toSet(),
    val setCount: Int = 7,
    val defaultBpm: Int = 92,
    val nameHint: String? = null,
)

data class ScaleInferenceResult(
    val candidates: List<ScaleCandidate>,
    val suggestedScale: ScaleDraft?,
)
