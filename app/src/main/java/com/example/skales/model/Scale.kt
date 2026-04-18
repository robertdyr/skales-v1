package com.example.skales.model

import java.util.UUID

enum class ScaleSoundKind {
    Cue,
    Note,
}

data class ScaleSound(
    val id: String = UUID.randomUUID().toString(),
    val notes: List<Int>,
    val kind: ScaleSoundKind,
    val breakAfterBeats: Float? = null,
)

data class ScaleSet(
    val sounds: List<ScaleSound>,
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
