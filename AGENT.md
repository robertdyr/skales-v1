# Skales — Agent Context

Read this before making changes.

## Product

Skales is an Android singing-practice app for building and replaying custom scale exercises.
The current product direction is playback-first: the domain should model what gets played, in what
grouping, and with what spacing.

There is no backend in this repo.

## Stack

- Kotlin
- Jetpack Compose + Material3
- MVVM
- Coroutines + StateFlow
- Room
- Navigation Compose
- Sample-based piano playback via `SoundPool`

## Domain Model

The scale model is now set-based and event-based.

```kotlin
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
```

Mental model:

- `C --- N ---- N ---- N -- N -- N`
- `C` means a cue sound event
- `N` means a normal note sound event
- `-` means beat-space after that sound
- scales are organized into sets, so the real shape is closer to:
  - `Set 1: C --- N ---- N`
  - `Set 2: C --- N -- N -- N`

Important details:

- A `ScaleSound` is one playback step.
- A sound may contain one note or many notes played together.
- Cue sounds are not special playback rules anymore; they are regular sounds with `kind = Cue`.
- Per-sound spacing lives on `ScaleSound.breakAfterBeats`.
- Per-set spacing lives on `ScaleSet.breakAfterBeats`.
- The only global timing value is BPM.

Defaults currently used by playback when breaks are omitted:

- note sound break: `1f`
- cue sound break: `1.5f`
- set break: `1.75f`

## Playback Architecture

Playback is split into three layers.

### `PianoSoundPlayer`

The dumb audio layer.

Responsibilities:

- load piano samples into `SoundPool`
- play a single sound event via `playSound(notes: List<Int>)`
- stop active streams
- release audio resources

It does not know about BPM, sets, or scheduling.

### `PlaybackCursor`

The shared playback state.

```kotlin
data class PlaybackCursor(
    val setIndex: Int = 0,
    val soundIndexInSet: Int = 0,
    val isFinished: Boolean = false,
)
```

This is the canonical "where are we in the scale?" state shared by:

- single-step playback
- autoplay playback

### `ScaleStepper`

Advances the cursor by one playable sound.

`next(scale, cursor)`:

- plays exactly one `ScaleSound`
- computes the wait in beats before the next step
- advances the cursor

### `ScaleAutoPlayer`

Automation around the stepper.

It repeatedly:

1. calls `ScaleStepper.next(...)`
2. converts beats to milliseconds using current BPM
3. suspends via `delay(...)`
4. continues until the cursor is finished or playback is canceled

This means BPM changes during playback affect future waits immediately.

## Persistence

- Room still stores persistence-only timestamps in `ScaleEntity`
- domain `Scale` does not contain `createdAt` / `updatedAt`
- `ScaleEntity` now stores `sets` and `defaultBpm`
- nested sets/sounds are serialized via Room type converters
- destructive migration is still enabled while the schema is changing quickly

## Current Editing Model

The editor is now set-first.

- scales are authored as sets containing sounds
- tapping the piano adds a note sound to the selected set
- `Add chord cue` prepends or replaces a cue sound in the selected set based on the set's note sounds
- playback controls expose:
  - step
  - play
  - stop
  - reset cursor

## Important Constraints

- Keep changes small and explicit.
- Prefer readable playback state over clever implicit scheduling.
- Do not put persistence metadata into domain models.
- Do not reintroduce the old flat `notes + setStarts + cue` shape unless explicitly requested.
