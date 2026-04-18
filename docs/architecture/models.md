# Domain Models

## Purpose

The core domain models for Skales live in `app/src/main/java/com/example/skales/model/`.

These models represent the final saved playback-ready exercise structure.

This document describes both:

- the stable product meaning of the core models
- the current architectural direction for how timing should be represented going forward

## Current Decision Direction

The project is moving toward a step-position event model as the long-term source of truth.

That means:

- a `ScaleSound` remains one playback event
- a sound may contain one note or multiple notes played together
- timing is better understood as the event's position on a stable internal grid
- BPM affects playback speed, not the structural meaning of the saved event order

This is different from the earlier `breakAfterBeats` direction, where each sound owned the gap after itself.

The reason for the shift is not that relative spacing is invalid. Relative spacing is musically sensible. The reason is that the rest of the product is increasingly centered around:

- piano-roll editing
- grouped simultaneous sounds
- inferred set insertion before or after confirmed anchors
- playback review from arbitrary regions
- future multi-note cue/chord authoring

Those workflows align better with absolute step placement than with relative gap ownership.

## Decision Summary

The model choice is best understood as a tradeoff between two valid approaches.

### Relative spacing model

Example direction:

```kotlin
data class ScaleSound(
    val breakAfterBeats: Float?,
)
```

Strengths:

- spacing semantics are elegant
- increasing one break naturally pushes all later events
- phrase-level timing edits are direct

Weaknesses for Skales:

- grouped simultaneous sounds are more awkward to edit
- the piano roll becomes a projection of a different underlying truth
- inference merge behavior is harder to reason about
- insertion before confirmed anchors is less natural
- boundary behavior becomes more derived and less direct

### Absolute step-position model

Example direction:

```kotlin
data class ScaleSound(
    val step: Int,
)
```

Strengths:

- matches piano-roll authoring directly
- grouped sounds naturally share one step
- inference can propose ordered events more easily
- inserting sets before or after confirmed anchors is simpler
- BPM changes stay global and straightforward

Weaknesses:

- phrase spacing is less directly expressed as "the gap after this sound"
- some spacing edits may need explicit repositioning rather than just incrementing one value
- migration from the current saved representation will require care

## Product Position

For this product, the likely long-term winner is the absolute step-position model.

Why:

- the editor is already grid-first in practice
- grouped sounds matter for cue chords and future inference output
- infer wants ordered structural placement more than local gap ownership
- absolute step positions make review, insertion, and regeneration easier to understand

This does not mean the earlier `breakAfterBeats` idea was wrong. It means the rest of the product has made the structural editing use case more important than the local-spacing use case.

## Scale Model

```kotlin
data class Scale(
    val id: String,
    val name: String,
    val sets: List<ScaleSet>,
    val timing: PlaybackTiming,
)
```

A `Scale` is the final saved playback object.

It represents one complete exercise.

## ScaleSet Model

```kotlin
data class ScaleSet(
    val sounds: List<ScaleSound>,
)
```

A `ScaleSet` is one grouped exercise unit within a scale.

Stable meaning:

- sets remain real exercise grouping
- sets are not just a UI convenience
- playback still traverses sets in order
- editor inference and review still reason about set-level anchors

Current direction:

- sets should remain grouping structure
- set boundaries should be derived from the first sound in the set
- timing should not depend on a second set-level break field

## ScaleSound Model

Current code still uses:

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

Architectural direction should be read as:

- `ScaleSound` is one playback event
- it has one shared start position on the exercise grid
- all notes inside that sound are played together
- `kind` distinguishes cue sounds from normal note sounds

Future likely direction:

```kotlin
data class ScaleSound(
    val id: String = UUID.randomUUID().toString(),
    val notes: List<Int>,
    val kind: ScaleSoundKind,
    val step: Int,
)
```

The exact field name may change. The important point is the semantic move:

- from relative post-gap ownership
- to absolute event placement on a stable internal step grid

## Event-Based First

The model remains event-based first.

That means:

- `ScaleSound` is still one event
- a sound can contain one note or multiple notes
- notes inside the same sound share one start position
- future note-level identity may be added inside the sound for richer grouped editing

That future note-level identity does not change the event-level meaning of `ScaleSound`.

## PlaybackTiming Model

```kotlin
data class PlaybackTiming(
    val defaultBpm: Int,
)
```

Stable meaning:

- BPM is global playback speed

Likely direction:

- BPM should scale playback over stable step positions
- BPM should not be the thing that defines structural ordering

## Timing Representation

The important design question is not "time-based or not".

The real comparison is:

- relative ordering through `breakAfterBeats`
- absolute ordering through stable step positions

The project is leaning toward absolute ordering on a stable step grid.

That step grid should be understood as:

- internal saved timing resolution
- independent of the currently selected UI snap mode

Important rule:

- do not make the visible editor snap setting the saved timing model

Instead:

- saved timing should use a stable internal step resolution
- UI snap should only constrain editing behavior and placement convenience

## Internal Step Resolution

The exact internal resolution is still open.

Good candidates:

- half-beat steps
- quarter-beat steps
- integer ticks with a small fixed base unit

The choice should optimize for:

- simple playback scheduling
- stable persistence
- editor drag behavior
- inference output structure
- grouped event authoring

The choice should not be driven only by the currently visible snap options.

## Mental Model

The long-term model should be understood like this:

```text
Set 1: step 0  -> CueChord
       step 2  -> Note
       step 4  -> Note

Set 2: step 6  -> Note
       step 8  -> CueChord
```

Key points:

- a sound lives at one step
- multiple notes in a sound all happen at that step
- spacing is derived from the difference between steps
- set starts are derived from the first sound step in each set

## Multi-Note Sound Rule

`ScaleSound.notes` is a real grouped event, not just a label convenience.

That means:

- a cue can contain multiple notes played together
- a normal note sound can also contain multiple notes if the exercise requires a grouped event
- the editor should render all pitches in that sound
- dragging the sound should transpose all of its pitches together by default
- grouped sounds should keep one shared start step

This grouped-event model is important for future inferred cue chords and other inferred simultaneous sounds.

## Future Note Identity

If grouped editing becomes richer, the likely next step is note identity inside a sound.

Example direction:

```kotlin
data class SoundNote(
    val id: String = UUID.randomUUID().toString(),
    val midi: Int,
)

data class ScaleSound(
    val id: String = UUID.randomUUID().toString(),
    val notes: List<SoundNote>,
    val kind: ScaleSoundKind,
    val step: Int,
)
```

That would support later behaviors such as:

- dragging one note into or out of a grouped sound
- deleting one note from a cue chord
- merging and splitting sounds more intentionally

This is not required to justify the move to absolute step positions. It is a separate future editing refinement.

## Why Not Absolute Time

The project should avoid a full absolute time-and-duration model unless scope changes dramatically.

Reasons:

- it pushes the app toward DAW complexity
- BPM-based speed changes become less conceptually direct
- set semantics become more derived
- grouped event editing becomes heavier than needed for exercise authoring

The preferred long-term target is:

- event-based model
- absolute step positions
- global BPM playback

not:

- free absolute time sequencing
- independent note durations everywhere

## Migration Note For Future Agent

The codebase still contains logic built around `breakAfterBeats`.

Do not assume that means the architecture decision is still open. The current documentation direction is that step-position storage is likely the better long-term model.

If implementing the migration:

1. choose and document the stable internal step resolution first
2. add step-based timing to the domain model
3. update playback to derive waits from adjacent steps
4. simplify editor projection logic so the grid is no longer rebuilding a different timing truth
5. migrate persistence carefully from relative gaps to steps
6. only then build richer grouped-sound editing and editor inference on top

## Important Constraints

- domain models do not contain persistence metadata such as `createdAt` or `updatedAt`
- persistence metadata lives in `ScaleEntity`
- the domain model should stay playback-oriented, not become a screen-state model
- do not casually reintroduce set-level timing breaks
- do not add full duration or velocity editing to v1 unless product scope changes intentionally

## Related Documentation

- `editor.md` - how these models are authored and edited
- `infer.md` - how partial exercises are completed and merged
- `player.md` - how step-based playback should consume the final model
- `storage.md` - how these models are persisted
