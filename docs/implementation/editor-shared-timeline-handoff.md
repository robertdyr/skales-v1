# Editor Shared Timeline Handoff

## Purpose

Short implementation notes for the current editor behavior.

## Core Rules

### Model

- saved structure is `Scale -> List<ScaleSet> -> List<ScaleSound>`
- the editor is a projection of that structure
- do not introduce a second persisted timeline model

### Timing

- each `ScaleSound` stores an absolute `step`
- there is no set-level break field
- spacing is derived from adjacent step positions

### Boundaries

- a set boundary is the start of that set's first sound
- dragging the separator moves the boundary explicitly
- dragging the first sound of a later set also moves that boundary
- dragging later sounds in that set only moves those sounds

### Same-Step Sounds

- same-step sounds are independent sounds
- playback groups them together
- editor rendering should keep them legible

## Current Rough Edges

- set boundary visuals can be clearer
- same-step note rendering can be more legible
- selected-set ownership could use clearer feedback

## Files To Read First

- `app/src/main/java/com/example/skales/app/screens/ScaleEditorScreen.kt`
- `app/src/main/java/com/example/skales/app/components/SetPianoRollEditor.kt`
- `app/src/main/java/com/example/skales/editor/SetGridOps.kt`
- `app/src/main/java/com/example/skales/editor/ScaleEditorOps.kt`
- `app/src/main/java/com/example/skales/app/viewmodel/ScaleEditorViewModel.kt`
