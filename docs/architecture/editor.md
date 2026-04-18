# Editor

## Responsibility

The editor is the authoring and correction surface for a `Scale`.

It owns:

- manual scale creation
- correction of inferred or saved scales
- projection of `Scale -> ScaleSet -> ScaleSound` into the piano-roll UI
- editing operations on sounds, boundaries, and selected sets

It does not own:

- inference logic
- persistence rules
- playback scheduling

## Code Mapping

- `editor/ScaleEditorOps.kt`
- `editor/SetGridOps.kt`
- `app/viewmodel/ScaleEditorViewModel.kt`
- `app/screens/ScaleEditorScreen.kt`
- `app/components/SetPianoRollEditor.kt`

## Core Editor View

The editor shows one shared piano-roll timeline for the entire scale.

That means:

- all sets are visible on one grid
- one set is selected for editing ownership
- non-selected sets remain visible as context
- set boundaries are visible grouping markers

## Timing Rules

The editor uses the saved absolute-step model directly.

- sounds live at stable internal step positions
- same-step sounds may overlap in time and play together
- spacing is derived from step differences
- changing snap does not rewrite saved positions

## Set Boundary Rules

- the boundary of a set is the first sound step in that set
- dragging the separator moves that boundary explicitly
- dragging the first sound of a later set also moves that boundary
- dragging later sounds within a set does not move the set boundary

## Editing Rules

- only the selected set is directly editable
- tapping a sound in another set switches selection to that set
- dragging a sound moves that sound on the grid
- same-step sounds remain independent sounds even when they play together
- keyboard input adds a sound to the selected set

## Same-Step Sounds

Current semantics:

- each sound is independent
- several sounds may share one `step`
- playback groups those sounds into one playback moment
- rendering should make same-step sounds readable without turning them into one object

## Review State

The saved model stays plain:

- `Scale`
- `ScaleSet`
- `ScaleSound`

Editor-specific review state such as confirmed or suggested should live outside the saved domain model.

## Non-Goals

Do not add by default:

- arbitrary duration editing
- velocity editing
- articulation tools
- a second saved timeline model
- DAW-style resizing tools

## Current Direction

Future editor work should focus on:

- confirmed vs suggested set state
- better local playback review
- better readability for same-step notes
- better ownership and boundary feedback
