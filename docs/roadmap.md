# Roadmap

## Goal

Build the app around the fastest useful creation loop first:

```text
editor -> infer -> correct -> save -> play
```

## Current Priority

Focus on making new scales easy to create and correct without recording-first workflows yet.

That means:

- fast manual editing
- partial-set inference
- review and correction in the same editor
- smooth save and playback handoff

## Phase 1

Solid editor-first creation.

- create scales manually
- place and drag notes in the piano roll
- support snap sizes `1/1`, `1/2`, and `1/4`
- keep all sets visible in one shared timeline
- save the result
- play the saved scale

## Phase 2

Make inference practical.

- confirmed vs suggested set state
- lock corrected sets
- reinfer unresolved sets
- probe suggestions
- fill-range suggestions

## Phase 3

Improve review and playback.

- local playback review from selected context
- same-step note rendering polish
- better dense-editing ergonomics
- clearer boundary and ownership feedback

## Phase 4

Improve inference quality.

- better proposal quality
- better ranking from partial evidence
- better naming suggestions

## Phase 5

Add analyzer-led workflows.

- imported audio -> evidence
- evidence -> infer draft
- correction in editor

## Decision Rule

If a new task appears, ask:

1. does it make editor-first creation easier?
2. does it make infer-and-correct workflows better?
3. does it improve practice playback?

If not, it is probably later work.
