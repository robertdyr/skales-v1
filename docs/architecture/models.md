# Domain Models

## Central Models

The core domain models for Skales live in `app/src/main/java/com/example/skales/model/`.

These models represent the final saved playback-ready scale structure.

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
)
```

A `ScaleSet` is one grouped exercise unit within a scale.

Important current rule:

- sets still exist as real domain grouping
- sets no longer carry their own timing break field
- the first sound of a set defines where that set visibly begins on the shared editor timeline

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

A `ScaleSound` is one playback event.

Important details:

- a sound can contain one note or multiple notes played together
- `kind` distinguishes cue sounds from normal note sounds
- cues are still regular sounds, not a second hidden structure
- `breakAfterBeats` controls the spacing after this sound
- for v1, all sounds are treated as having the same played duration; only the spacing to the next sound is modeled

This model is intentionally event-based first.

- `ScaleSound` represents one shared start time
- all notes inside that sound are played together as one event
- future editor work may introduce note-level identity inside a sound, but that does not change the event-level meaning of `ScaleSound`

## PlaybackTiming Model

```kotlin
data class PlaybackTiming(
    val defaultBpm: Int,
)
```

The only global timing value is BPM. All spacing is beat-based.

## Mental Model

Think of a scale as a sequence of sound events with beat-based spacing:

```text
C --- N ---- N ---- CueChord -- N
```

- `C` = cue sound
- `N` = normal note sound
- `CueChord` = one cue event containing multiple notes
- `-` = beat-space after that sound

Sets group these sounds, but the editor now renders them on one shared timeline:

```text
Set 1: C --- N ---- N
Set 2: CueChord -- N -- N
```

There is no extra set break field anymore. Cross-set spacing is still represented by the previous sound's `breakAfterBeats`.

## Spacing Rules

Only one spacing rule matters now:

- per-sound spacing: `ScaleSound.breakAfterBeats`

Default break when omitted:

- note sound: `1f` beat
- cue sound: `1f` beat

Current product decision:

- cue sounds do not get a hidden longer default pause
- if a cue should have more space after it, that should be expressed explicitly in the editor/model

## Multi-Note Sound Rule

`ScaleSound.notes` is a real grouped event, not just a label convenience.

That means:

- a cue can contain multiple notes played together
- a normal note sound can also contain multiple notes if the exercise requires a grouped event
- the editor should render all pitches in that sound
- dragging the sound should transpose all of its pitches together
- grouped sounds should keep one shared start time and one shared `breakAfterBeats`

This grouped-event model is important for future inferred cue chords and other inferred simultaneous sounds.

## Note Representation

Notes are represented as MIDI note numbers (0-127).

Examples:

- Middle C (C4) = 60
- A4 (440 Hz) = 69

## Important Constraints

- domain models do not contain persistence metadata (`createdAt`, `updatedAt`)
- persistence metadata lives in `ScaleEntity` (see `storage.md`)
- the domain model is playback-focused: it models what gets played, in what grouping, and with what spacing
- do not reintroduce set-level break timing
- do not add per-sound duration or velocity to v1 unless product scope changes intentionally

## Current Model Direction

The current model should be understood as:

- grouping is set-based
- timing is per-sound
- editing is shared-timeline

That combination is deliberate.

## Related Documentation

- `player.md` - how these models are consumed for playback
- `editor.md` - how these models are authored and edited
- `infer.md` - how partial scales are completed
- `storage.md` - how these models are persisted
