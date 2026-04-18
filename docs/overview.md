# Skales Overview

## Product Summary

Skales is an Android app for creating, inferring, saving, and replaying custom singing-practice scales.

The current product focus is editor-first creation with inference support and repeatable playback.

## Core Flow

```text
enter notes -> adjust timing on the grid -> infer missing sets -> review -> save -> play
```

## Current Focus

- shared-timeline piano-roll editing
- absolute-step timing
- partial-set inference
- save and playback of authored scales

## Core User Jobs

The app should help the user:

- create a scale manually
- place and drag notes on a piano-roll grid
- group notes into sets while still seeing the full exercise on one timeline
- infer missing sets from partial work
- review and correct results before saving
- replay saved scales for practice

## Stable Product Decisions

- `Scale` is the saved playback object
- `ScaleSet` remains real grouping structure
- `ScaleSound` is one note event
- simultaneity is represented by same-step sounds
- timing is stored as absolute `step` positions
- UI snap is an editing aid, not saved timing truth
- manual correction happens in the editor, not in a separate model

## Current State

Implemented now:

- scale library screen
- shared-timeline editor
- floating playback, grid, and set controls in the editor
- attached piano keyboard under the grid
- scale player
- local persistence with Room
- deterministic analyzer pipeline for note evidence extraction
- deterministic infer support for draft generation from evidence or partial sets

Current gaps:

- confirmed vs suggested set state in the editor
- richer editor-driven inference flow
- localized playback review during correction
- polish for same-step note rendering and dense editing cases

## In Scope

- editor-first scale authoring
- partial-set inference and reinference
- repeatable practice playback
- recording-led product direction as a future constraint

## Out Of Scope For Now

- microphone capture workflow in the MVP
- cloud sync
- collaboration
- advanced DAW-style editing

## Reading Order

1. `architecture/overview.md`
2. `architecture/models.md`
3. `architecture/editor.md`
4. `architecture/infer.md`
5. `architecture/player.md`
6. `roadmap.md`
