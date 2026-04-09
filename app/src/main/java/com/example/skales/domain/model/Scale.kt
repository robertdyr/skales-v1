package com.example.skales.domain.model

enum class ScaleSoundKind {
    Cue,
    Note,
}

data class ScaleSound(
    val notes: List<Int>,
    val kind: ScaleSoundKind,
    val breakAfterBeats: Float? = null,
)

data class ScaleSet(
    val sounds: List<ScaleSound>,
    val breakAfterBeats: Float? = null,
)

data class PlaybackTiming(
    val defaultBpm: Int,
)

data class Scale(
    val id: String,
    val name: String,
    val sets: List<ScaleSet>,
    val timing: PlaybackTiming,
)
