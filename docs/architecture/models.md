# Domain Models

## Central Models

The core domain models for Skales live in `app/src/main/java/com/example/skales/model/`.

These models represent the final playback-ready scale structure.

## Scale Model

```kotlin
data class Scale(
    val id: String,
    val name: String,
    val sets: List<ScaleSet>,
    val timing: PlaybackTiming,
)
```

A `Scale` is the final saved playback object. It represents one complete exercise.

## ScaleSet Model

```kotlin
data class ScaleSet(
    val sounds: List<ScaleSound>,
    val breakAfterBeats: Float? = null,
)
```

A `ScaleSet` is one repeatable exercise unit within a scale.

Scales are organized into sets, so a real exercise looks like:

- `Set 1: C --- N ---- N`
- `Set 2: C --- N -- N -- N`

The `breakAfterBeats` field adds extra spacing after the final sound in the set.

## ScaleSound Model

```kotlin
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
```

A `ScaleSound` is one playback step.

Important details:

- A sound can contain one note or multiple notes played together (chords)
- `kind` distinguishes between cue sounds and normal note sounds
- Cue sounds are regular sounds with `kind = Cue` (no special playback rules)
- `breakAfterBeats` controls spacing after this sound

## PlaybackTiming Model

```kotlin
data class PlaybackTiming(
    val defaultBpm: Int,
)
```

The only global timing value is BPM. All spacing is beat-based.

## Mental Model

Think of a scale as a sequence of sounds with beat-based spacing:

```text
C --- N ---- N ---- N -- N -- N
```

- `C` = cue sound
- `N` = normal note sound
- `-` = beat-space after that sound

Scales are organized into sets:

```text
Set 1: C --- N ---- N
Set 2: C --- N -- N -- N
```

## Spacing Rules

Per-sound spacing: `ScaleSound.breakAfterBeats`
Per-set spacing: `ScaleSet.breakAfterBeats`

Default breaks when omitted:

- note sound: `1f` beat
- cue sound: `1.5f` beats
- set break: `1.75f` beats (added after the final sound in the set)

## Note Representation

Notes are represented as MIDI note numbers (0-127).

Example:
- Middle C (C4) = 60
- A4 (440 Hz) = 69

## Important Constraints

- Domain models do not contain persistence metadata (`createdAt`, `updatedAt`)
- Persistence metadata lives in `ScaleEntity` (see `storage.md`)
- The domain model is playback-focused: it models what gets played, in what grouping, and with what spacing
- Do not reintroduce the old flat `notes + setStarts + cue` shape

## Model Evolution

The current model is set-based and sound-based. This replaced an earlier flat representation.

The set-based model supports:

- clear grouping of related sounds
- explicit per-sound and per-set spacing
- easy editing and inference
- straightforward playback scheduling

## Related Documentation

- `player.md` - how these models are consumed for playback
- `editor.md` - how these models are authored and edited
- `infer.md` - how partial scales are completed
- `storage.md` - how these models are persisted
