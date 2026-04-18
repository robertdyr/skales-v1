# Editor

## Responsibility

The editor is the authoring and correction surface for a `Scale`.

It owns:

- manual scale creation
- correction of saved or inferred scales
- projection of `Scale -> ScaleSet -> ScaleSound` into an editable piano-roll view

It does not own:

- inference logic
- audio analysis
- persistence details

## Code Mapping

- `editor/ScaleEditorOps.kt`
- `editor/SetGridOps.kt`
- `app/viewmodel/ScaleEditorViewModel.kt`
- `app/screens/ScaleEditorScreen.kt`
- `app/components/SetPianoRollEditor.kt`
- `app/components/PianoKeyboard.kt`

Split of responsibility:

- `editor/` owns pure editing and timeline projection logic
- `ScaleEditorViewModel` owns editor session state
- `app/screens` and `app/components` own UI rendering and input

## Model

The saved model stays:

```text
Scale -> List<ScaleSet> -> List<ScaleSound>
```

The editor presents that model as one shared piano-roll timeline.

That means:

- all sets are shown on one grid
- one set is selected for editing ownership
- non-selected sets remain visible as context
- set boundaries are derived from the first sound of each set

The grid is a projection of the saved model, not a second persisted model.

## Timing

For v1, timing is intentionally simple.

- `ScaleSound.breakAfterBeats` is the only editable spacing value
- all sounds are treated as having the same played length
- there is no set-level break field

The key rule is:

- spacing between two adjacent sounds is owned by the earlier sound

That includes spacing across a set boundary.

So:

- the gap between the last sound of set A and the first sound of set B is stored on the last sound of set A
- moving a set start later increases that previous break
- moving a set start earlier decreases that previous break

## Shared Timeline Rules

- sets remain real grouping structure
- the editor behaves like one timeline with visible set boundaries
- the boundary of a set is the start time of that set's first sound
- cues and grouped sounds remain first-class events

When the user edits the grid, timing is rebuilt from the resulting timeline order and then written back into the owning sets.

## Editing Rules

- only the selected set is directly editable
- tapping a sound in another set switches selection to that set
- dragging a sound usually changes only that sound's position and local spacing
- dragging the first sound of a later set changes that set's start because it defines the boundary
- dragging a separator is the explicit control for moving a set boundary

Within a set, normal note drags should not move the boundary unless the dragged sound is the first sound of that set.

## Rendering Rules

- note sound: square block
- cue sound: circular block
- multi-note sound: one event with multiple visible pitches
- grouped sounds transpose together when dragged vertically

## Non-Goals

The editor should not become a DAW.

Do not add by default:

- per-sound duration editing
- velocity editing
- articulation tools
- hidden cue-specific timing rules
- a second saved timeline model

## Future Role

The editor is still the expected correction surface for later reinference work.

Likely additions later:

- inferred vs confirmed set state
- lock and unlock controls for sets
- infer-missing-sets actions
- better review handoff for inferred drafts
