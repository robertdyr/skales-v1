# Player

## Responsibility

The player turns a final `Scale` into audible playback.

It should prefer operation-style APIs over owning screen session state.

## Main Parts

### `ScaleStepper`

Owns:

- one playback step at a time
- next-cursor calculation
- per-step wait calculation

### `ScaleAutoPlayer`

Owns:

- repeated stepping over time
- BPM-based scheduling
- stop and cancel behavior

### `PianoSoundPlayer`

Owns:

- actual piano sample playback
- note and chord playback output

## Input Model

The player consumes the saved playback model:

- `Scale`
- `ScaleSet`
- `ScaleSound`
- `PlaybackTiming`

## Timing Direction

The player should increasingly assume a step-based event model.

That means:

- each sound has a stable step position
- grouped sounds play all their notes at that step
- wait duration is derived from the difference between adjacent event steps
- BPM scales playback speed globally across those step differences

This is simpler and more structurally aligned with the editor than treating the saved model as a chain of relative post-gaps.

## Why This Matters

The player should not need to know or care whether a sound was:

- manually authored
- inferred
- accepted from a suggestion

It should simply consume:

- ordered sets
- ordered sounds within those sets
- stable positions

That makes later localized playback review easier as well.

## What The Player Must Not Know

- whether the scale was manually authored or inferred
- raw note-detection evidence
- Room entities
- screen-level navigation state
- editor review state such as confirmed versus suggested

## Future Direction

The player will likely need better support for:

- play from selected set
- later, possibly play from selected sound
- local preview while reviewing suggested sets

Those are application-layer cursor and review concerns on top of the same playback model, not a different playback model.

## Migration Note For Future Agent

The current playback code still computes waits from `breakAfterBeats`.

If the model migration to absolute step positions happens, update player logic to:

1. sort playable sounds by structural order
2. compute adjacent step deltas
3. convert step delta plus BPM into wait duration
4. keep grouped sounds as one playback event that can contain multiple notes

Avoid migrating the player in isolation. It should move together with the final timing-model decision in `models.md`.
