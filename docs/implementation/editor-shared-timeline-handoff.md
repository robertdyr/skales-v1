# Editor Shared Timeline Handoff

## Purpose

This document is a short implementation handoff for the current editor behavior.

Read this after:

- `docs/architecture/editor.md`
- `docs/ui-ux/editor-screen.md`

## Current Direction

The editor has moved away from isolated per-set editing.

The implemented direction is:

- one shared piano-roll timeline for all sets
- one selected set for editing ownership
- keyboard attached directly under the grid
- floating playback, grid, and set controls

Sets still exist as real domain grouping. The editor simply shows them on one timeline.

## Core Implementation Rules

### Model

- saved structure remains `Scale -> List<ScaleSet> -> List<ScaleSound>`
- the grid is a projection of that structure
- do not introduce a second persisted timeline model

### Timing

- `breakAfterBeats` is the only spacing value
- there is no set-level break field
- cross-set spacing is still stored on the earlier sound

### Boundaries

- a set boundary is the start of that set's first sound
- dragging the separator moves the boundary explicitly
- dragging the first sound of a later set also moves that boundary
- dragging later sounds in that set should only rewrite in-set spacing

### Rebuild

When a sound moves:

- project all sounds onto the shared timeline
- apply the edit in timeline space
- rebuild `breakAfterBeats` from adjacent sounds
- write the rebuilt sounds back into their owning sets

## What Is Working Well

- the full-height editor surface is stronger than the old card-and-dock layout
- shared-timeline editing makes set relationships easier to read
- attached keyboard works better on phone than the old dock
- grouped cues as real draggable sounds is the right direction

## Current Rough Edges

- set boundary visuals can still be clearer
- grouped cue rendering can still be more legible
- overlay spacing and density can still be polished
- selected-set ownership could still use clearer feedback

## Non-Goals

Do not casually add:

- duration editing
- velocity editing
- articulation controls
- hidden cue-specific pauses
- set-level break timing

## Files To Read First

- `app/src/main/java/com/example/skales/app/screens/ScaleEditorScreen.kt`
- `app/src/main/java/com/example/skales/app/components/SetPianoRollEditor.kt`
- `app/src/main/java/com/example/skales/editor/SetGridOps.kt`
- `app/src/main/java/com/example/skales/editor/ScaleEditorOps.kt`
- `app/src/main/java/com/example/skales/app/viewmodel/ScaleEditorViewModel.kt`
