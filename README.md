# Skales

Skales is an Android app for building and replaying custom singing-practice scales.

## Domain Model

The exercise model is set-based and sound-based.

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
- `C` is a cue sound
- `N` is a normal note sound
- `-` is beat-space after the sound

Scales are organized into sets, so a real exercise looks more like:

- `Set 1: C --- N ---- N`
- `Set 2: C --- N -- N -- N`

Each `ScaleSound` is one playback step. A sound can contain one note or several notes played at the same time.

## Timing

The only global timing value is BPM.

Per-event spacing is explicit:

- `ScaleSound.breakAfterBeats` controls break after one sound
- `ScaleSet.breakAfterBeats` adds extra break after the final sound in the set

Default playback spacing when a break is omitted:

- note: `1f`
- cue: `1.5f`
- set: `1.75f`

## Playback Architecture

Playback is split into three parts.

### `PianoSoundPlayer`

The dumb audio layer.

- loads piano samples into `SoundPool`
- plays a sound via `playSound(notes: List<Int>)`
- stops active streams
- releases audio resources

### `PlaybackCursor`

The shared global playback position.

```kotlin
data class PlaybackCursor(
    val setIndex: Int = 0,
    val soundIndexInSet: Int = 0,
    val isFinished: Boolean = false,
)
```

Both step playback and autoplay advance the same cursor.

### `ScaleStepper`

Single-step playback.

- plays exactly one `ScaleSound`
- advances the cursor
- returns how many beats autoplay should wait before the next step

### `ScaleAutoPlayer`

Autoplay scheduler.

- repeatedly calls the stepper
- converts beats to milliseconds from the current BPM
- suspends with `delay(...)` between steps

This lets BPM changes take effect during playback without rebuilding a full queue.

## Project Notes

- Android only
- No backend in this repo
- Room is used for local persistence
- destructive migration is currently enabled while the schema is still evolving
