# Player

## Responsibility

The player is the library that turns a final `Scale` into audible playback.

## External Contract

```text
Input  : Scale
Output : sound playback
```

## Internal Shape

```text
+-------------------- player --------------------+
|                                                |
|  Scale -> stepper -> scheduler -> sound player |
|             |                    |             |
|             +---- cursor state ---+            |
|                                                |
+------------------------------------------------+
```

## Main Parts

### `ScaleStepper`

Owns:

- one playback step at a time
- cursor advancement
- per-step wait calculation

### `ScaleAutoPlayer`

Owns:

- repeated stepping over time
- BPM-based scheduling
- stop/cancel behavior

### `PianoSoundPlayer`

Owns:

- actual piano sample playback
- note/chord playback output

## Input Model

The player consumes the saved playback model:

- `Scale`
- `ScaleSet`
- `ScaleSound`
- `PlaybackTiming`

## What The Player Must Not Know

- whether the scale was manually authored or analyzed from audio
- raw note-detection evidence
- Room entities
- screen-level navigation state

## Current Code Mapping

```text
player/
├── PianoSoundPlayer.kt
├── ScaleAutoPlayer.kt
└── ScaleStepper.kt
```

## Current Behavior

```text
Scale
  -> Play / Step
  -> Forward or Backward
  -> audible practice playback
```

## Future Extension Points

- playback preview in draft review
- richer break timing support
- transpose at playback time
- metronome or count-in cue
