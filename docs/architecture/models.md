# Domain Models

## Purpose

These are the saved playback models in `app/src/main/java/com/example/skales/model/`.

They are the source of truth for editor, player, infer, and storage.

## Current Model

```kotlin
enum class ScaleSoundKind {
    Cue,
    Note,
}

data class ScaleSound(
    val id: String = UUID.randomUUID().toString(),
    val midi: Int,
    val kind: ScaleSoundKind,
    val step: Int = 0,
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
```

## Meaning

### `Scale`

- one saved exercise
- owns name, sets, and default playback tempo

### `ScaleSet`

- one structural group inside the exercise
- sets remain real domain structure even though the editor shows one shared timeline
- a set boundary is derived from the first sound in that set

### `ScaleSound`

- one note event
- `midi` is the pitch
- `step` is the absolute timeline position
- `kind` distinguishes cue sounds from normal note sounds

Important rule:

- simultaneity is represented by multiple `ScaleSound`s at the same `step`

### `PlaybackTiming`

- global playback tempo configuration

## Timing Model

The saved model is absolute-step based.

That means:

- each sound stores its own `step`
- spacing is derived from differences between adjacent steps
- snap is an editing aid, not saved timing truth
- BPM scales playback speed over the stored steps

## Internal Resolution

Current internal resolution:

- `1 step = 0.25 beats`
- `4 steps = 1 beat`

This is independent from the current snap setting in the editor.

## Simultaneity Rule

The canonical representation is:

- one sound = one note
- same-step sounds = notes that play together

This keeps the model aligned with current editor behavior.

## Non-Goals In The Model

The core model does not currently represent:

- per-note duration
- velocity
- articulation
- grouped chord objects inside one `ScaleSound`

If grouped chord objects are added later, they should be introduced explicitly with clear interaction semantics rather than implied by the current shape.

## Constraints

- domain models do not contain Room metadata such as `createdAt`
- persistence metadata belongs in storage entities
- do not reintroduce relative-gap timing into the saved model
- do not add DAW-style timing concepts unless product scope changes intentionally
