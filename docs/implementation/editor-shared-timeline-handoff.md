# Editor Shared Timeline Handoff

## Purpose

This document is a handoff for future agents or contributors who were not part of the editor redesign conversation.

It describes the current editor direction, what has been intentionally simplified for v1, and which rough edges are still worth iterating on.

## What Changed

The editor used to be documented as:

- one active-set piano roll
- bottom-attached dock for playback, keyboard, and sets
- set switching as the primary editing mode

The implemented direction is now:

- one shared piano-roll timeline for all sets
- keyboard attached directly under the grid
- floating overlays on the right for playback, grid, and sets
- selected set owns edits, but other sets remain visible on the same timeline

This shift is intentional.

## Current Product Rules

These are current design decisions, not open questions.

1. Sets still exist.
- `ScaleSet` remains real grouping structure.
- The app is not moving to a flat sequence model.

2. There is no set-level break anymore.
- Timing between sets is represented only by per-sound `breakAfterBeats`.

3. The first sound of a set defines where that set starts.
- That sound can be a cue or a normal note.
- The set boundary is visual, not a second timing object.
- In practice, the separator line is treated as a stable anchor unless an explicit boundary-moving interaction occurs.

4. The editor is not a DAW.
- Do not add velocity, articulation, per-sound duration, or complex resize tools for v1.

5. All sounds currently have the same played length conceptually.
- Only spacing to the next sound matters.

6. Cues are still real sounds.
- They are not just annotations.
- They can contain multiple notes.
- They should be editable like notes, while still rendering differently.

## Current Timing Model

For v1:

- each `ScaleSound` has `notes`, `kind`, and `breakAfterBeats`
- `breakAfterBeats` is the only spacing value
- spacing between adjacent sounds, even across set boundaries, is owned by the earlier sound

Important implication:

- moving the separator for set B later should increase the `breakAfterBeats` of the last sound of set A
- moving it earlier should decrease that same previous break
- dragging a later-set sound can also move that set's separator with it

This is deliberate. Do not reintroduce set-level break timing unless product direction changes.

## Current Rendering Model

The piano roll shows all sets on one timeline.

- selected set: brighter, editable
- other sets: dimmer, visible as context
- note sound: square block
- cue sound: circular block
- multi-note cue: grouped circles at one time position
- set boundaries: horizontal divider lines where each set starts

The keyboard is attached directly under the grid and shares the horizontal pitch scroll.

## Current Interaction Model

- keyboard appends to the selected set
- selected-set sounds are draggable
- non-selected sounds are visible but not directly draggable
- tapping a sound in another set should switch the working set immediately
- dragging a grouped cue transposes the whole cue together
- separator lines are the main explicit control for between-set spacing
- note dragging inside a set should not move the separator by default
- grid snap changes placement/drag behavior only; it must not rewrite timing globally

## Architectural Interpretation

The editor behaves like a shared timeline, but the saved model is still `Scale -> List<ScaleSet> -> List<ScaleSound>`.

Current rebuild rule:

- the grid projects all sounds onto one timeline
- when a sound moves, timing should be rebuilt from adjacent sounds across that full timeline
- rebuilt sounds are then written back into their owning sets

This shared-timeline rebuild is the main conceptual change from the older selected-set-only editor.

## What Is Working Well

- full-screen editor surface feels better than the old card + dock layout
- keyboard directly attached to the grid is much better on phone
- floating overlays fit the new layout better than the bottom dock
- shared timeline makes set relationships much easier to see
- cue sounds being rendered and draggable as grouped sounds is the right direction

## Known Rough Edges Worth Iterating On

These are legitimate next-pass improvement areas.

1. Cross-set spacing still needs careful UX verification.
- The intended rule is separator-first spacing with `breakAfterBeats` still stored on the previous sound.
- Verify note drags versus separator drags do not fall into conflicting states.

2. Set boundary visuals can still improve.
- Boundaries are conceptually correct, but may need stronger or clearer presentation.

3. Grouped cue rendering can be more legible.
- The current grouped circles are functionally correct.
- They may still want better clustering, spacing, or selection styling.

4. Overlay layout can still be polished.
- Playback, grid, and sets overlays are directionally correct.
- Their spacing, alignment, and density can still be improved.

5. The selected-set ownership rule may need clearer feedback.
- Right now non-selected sets are visible but not editable.
- Tapping another set's note should switch sets directly, but the visual feedback could still be stronger.

## Explicit Non-Goals For The Next Agent

Do not casually add these:

- duration editing
- velocity editing
- articulation controls
- full DAW-style resizing
- hidden cue-specific default pauses
- set-level break timing

These were discussed and intentionally deferred.

## Good Next Iteration Targets

Good next tasks:

1. polish grouped cue visuals and selection behavior
2. verify and tighten cross-set gap editing
3. improve set overlay summaries now that all sets share one timeline
4. return to inference flow once editor behavior is stable

## Files To Read First

If you are picking this up fresh, start here:

- `docs/ui-ux/editor-screen.md`
- `docs/architecture/editor.md`
- `docs/architecture/models.md`
- `app/src/main/java/com/example/skales/app/screens/ScaleEditorScreen.kt`
- `app/src/main/java/com/example/skales/app/components/SetPianoRollEditor.kt`
- `app/src/main/java/com/example/skales/editor/SetGridOps.kt`
- `app/src/main/java/com/example/skales/editor/ScaleEditorOps.kt`
- `app/src/main/java/com/example/skales/app/viewmodel/ScaleEditorViewModel.kt`
