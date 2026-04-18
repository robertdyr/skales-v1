# Player

## Responsibility

The player turns a saved `Scale` into audible playback.

## Main Parts

### `ScaleStepper`

Owns:

- one playback step at a time
- next-cursor calculation
- per-step wait calculation
- grouping same-step sounds into one playback moment

### `ScaleAutoPlayer`

Owns:

- repeated stepping over time
- BPM-based scheduling
- stop and cancel behavior

### `PianoSoundPlayer`

Owns:

- actual piano sample playback
- playing one or more notes for the current playback moment

## Input Model

The player consumes:

- `Scale`
- `ScaleSet`
- `ScaleSound`
- `PlaybackTiming`

## Playback Rules

- each sound has a stable absolute `step`
- sounds at the same `step` are played together
- wait duration is derived from the difference between adjacent steps
- BPM scales playback speed globally

## Cursor Model

The player uses a playback cursor, but that cursor is runtime state, not persisted domain state.

## What The Player Must Not Know

- analyzer internals
- Room entities
- editor UI state
- review state such as confirmed vs suggested

## Current Direction

Future player work should focus on:

- play from selected set
- later, possibly play from selected sound
- local preview during review flows
- playback polish for dense same-step note groups
