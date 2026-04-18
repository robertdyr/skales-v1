# Editor

## Responsibility

The editor is the authoring and correction surface for a `Scale`.

It owns:

- manual scale creation
- correction of saved or inferred scales
- projection of `Scale -> ScaleSet -> ScaleSound` into an editable piano-roll view
- review of suggested sets inside the same working surface

It does not own:

- inference logic itself
- audio analysis
- persistence rules

## Code Mapping

- `editor/ScaleEditorOps.kt`
- `editor/SetGridOps.kt`
- `app/viewmodel/ScaleEditorViewModel.kt`
- `app/screens/ScaleEditorScreen.kt`
- `app/components/SetPianoRollEditor.kt`
- `app/components/PianoKeyboard.kt`

Responsibility split:

- `editor/` owns editing and transformation logic
- `ScaleEditorViewModel` owns editor session state
- `app/screens` and `app/components` own UI rendering and input

## Core Model View

The saved model remains:

```text
Scale -> List<ScaleSet> -> List<ScaleSound>
```

The editor should continue to present that model as one shared piano-roll timeline.

That means:

- all sets are shown on one grid
- one set is selected for editing ownership
- non-selected sets stay visible as context
- set boundaries are visible grouping markers in one continuous exercise view

## Timing Direction

The editor should increasingly treat the grid as the structural source of truth.

That means the long-term model direction is:

- sounds live at stable internal step positions
- grouped sounds share one start step
- spacing is derived from differences between steps

This is important because the editor is already fundamentally working in:

- columns
- snapping
- boundary placement
- drag-based repositioning

The earlier `breakAfterBeats` model gave elegant local spacing semantics, but it makes the piano roll a projection of a different storage truth. That becomes increasingly awkward as the editor grows more structural.

## Why Step-Based Timing Fits The Editor

The editor increasingly needs to support:

- grouped simultaneous sounds
- moving sounds directly on the grid
- reordering by position
- inserting inferred sets before or after confirmed anchors
- reviewing only a local region of a larger exercise

Those are all more naturally expressed with absolute step positions than with relative post-gap ownership.

## Shared Timeline Rules

- sets remain real grouping structure
- the editor behaves like one timeline with visible set boundaries
- the boundary of a set is the start step of that set's first sound
- cues and grouped sounds remain first-class events
- grouped sounds are one event with multiple pitches, not multiple unrelated timeline items

## Set Boundaries

Set boundaries are visible grouping markers, not a second timing structure.

The boundary of a set is derived from the first sound in that set.

Practical implications:

- dragging the first sound of a later set moves that set start
- dragging later sounds within a set should not move the set start
- dragging the separator is an explicit boundary move

This rule becomes cleaner once the saved model stores event positions directly rather than reconstructing them from previous breaks.

## Review State

The saved domain model should remain a plain ordered list of `ScaleSet`s.

The editor may track additional per-set review metadata without changing that saved shape.

Examples:

- confirmed set
- suggested set
- later, locked set

Important rules:

- confirmed and suggested sets should appear in one working sequence on the same timeline
- inference should not open a second disconnected draft editor for normal correction work
- direct user edits to a suggested set should promote it to confirmed
- confirmed sets are trusted anchors during inference and reinference

## Editing Rules

- only the selected set is directly editable
- tapping a sound in another set switches selection to that set
- dragging a sound should move that event on the grid
- dragging grouped sounds should move their pitches together by default
- dragging later sounds within a set should not move the set boundary
- dragging the first sound of a later set changes that set start
- dragging a separator is the explicit boundary control

## Multi-Note Sound Direction

The editor should continue to treat a multi-note sound as one event at one step.

Short-term expectations:

- render all pitches in the sound
- move the sound as one unit by default
- allow inference to produce cue chords and other grouped simultaneous sounds

Later editing improvements may add note-level identity inside the sound. That is a refinement on top of the event model, not a replacement for it.

## Playback Review Direction

The editor should support local review, not only full-from-start playback.

Why:

- suggestion review becomes tedious if every edit requires replaying the whole exercise
- confirmed anchors may live inside a larger inferred range
- users need to test local suggestions before requesting wider generation

Likely direction:

- play from selected set
- later, possibly play from selected sound

## Non-Goals

The editor should not become a DAW.

Do not add by default:

- arbitrary duration editing
- velocity editing
- articulation tools
- free absolute time editing
- a second saved timeline model

## Migration Note For Future Agent

The current implementation still contains logic that reconstructs a shared timeline from `breakAfterBeats`.

That should be treated as implementation debt, not as the final conceptual model.

If continuing editor work:

1. align new editor behaviors with step-based event placement
2. avoid deepening assumptions that previous-sound gap ownership is the long-term truth
3. prefer designs that map directly to stable event positions and grouped events

## Future Role

The editor remains the correction surface for reinference work.

Likely additions later:

- inferred vs confirmed set state
- lock and unlock controls
- grouped-sound authoring and editing
- probe suggestions for a few next sets
- larger range fill actions around confirmed anchors
- better localized playback review during inference
