# Editor Screen

## Purpose

The editor is the main authoring, correction, and future reinference surface.

It is no longer a card-with-dock layout. The piano roll and keyboard are now the primary surface, and everything else should stay secondary to that editing area.

## Current Layout

```text
+---------------- top bar ----------------+
| Back   New scale / Edit scale    Save   |
+----------------------------------------+

+--------------- metadata ----------------+
| scale name field only                   |
+----------------------------------------+

+------------- shared timeline -----------+
| full-height piano roll                  |
| all sets rendered on one grid           |
| time runs upward                        |
| pitch runs left-right                   |
| selected set is editable                |
| other sets stay visible as context      |
| set boundaries are visual only          |
| floating right controls: play/grid/set  |
+----------------------------------------+

+--------------- keyboard ----------------+
| attached directly under the grid        |
| shares horizontal pitch scroll          |
| sits near bottom safe area              |
+----------------------------------------+
```

## UX Priorities

1. keep the piano roll and keyboard as the dominant surface
2. make set grouping visible without splitting editing into separate screens
3. keep playback, grid controls, and set controls reachable without restoring the old dock
4. make cue sounds first-class editing items rather than special hidden metadata
5. make spacing between sounds, including across set boundaries, readable and editable
6. avoid introducing DAW-level complexity for v1
7. keep save reachable in the app bar

## Core Interaction Model

The editor now works as one shared timeline with set grouping overlaid on top.

- all sets are shown on the same piano-roll grid
- the selected set is highlighted and editable
- non-selected sets are still visible in dimmer colors
- set boundaries are derived from the first sound of each set
- tapping a sound in another set makes that set the working set immediately
- note taps are owned by note blocks, not by opening the sets overlay first
- separator lines are the primary control for between-set spacing
- dragging the separator moves the start of that set and later sets together
- dragging a note inside its set does not move the separator by default
- dragging a set-A sound across the separator can push the separator upward as a special case
- dragging any sound in a later set moves that set's separator with it
- sounds cannot be dragged past neighboring sounds in a way that breaks ordering
- keyboard input appends to the selected set

## Timing Model

For v1, timing stays intentionally simple.

- every `ScaleSound` has one timing value: `breakAfterBeats`
- all sounds are treated as having the same played length
- there is no separate duration editing yet
- there is no velocity or articulation editing
- there is no set-level break anymore

Important consequence:

- the visible gap between two sounds is controlled by the earlier sound's `breakAfterBeats`
- the gap between the last sound of set A and the first sound of set B is still represented by the last sound of set A
- moving a separator later or earlier should increase or decrease the previous sound's `breakAfterBeats`
- moving a later-set sound moves that set start with it, which also changes the previous sound's `breakAfterBeats`

## Sound Rendering Rules

Each `ScaleSound` is one timeline event.

- normal note sound with one pitch: square block
- cue sound with one pitch: circular block
- cue sound with multiple pitches: grouped circles at the same time position
- multi-note sounds move together as one event
- vertical drag transposes all notes in that sound together
- horizontal/temporal drag changes the event's position on the shared timeline

## Grid Rule

The `Grid` floating control changes editor snapping only.

- `1/2` is the default snap
- `1/1` and `1/4` are optional snaps
- changing snap affects placement and drag quantization only
- changing snap must not rewrite saved timing automatically

## Playback Rule

Playback is a floating overlay, not a dock.

- primary visible playback action is play/stop
- secondary playback actions live in a compact popout
- cue sounds do not get a hidden longer fallback pause anymore
- what the user edits in the grid should match what they hear in playback timing

## Sets Rule

Sets are still part of the model and the product, but they are no longer isolated piano-roll workspaces.

- the set overlay selects the active set
- tapping a sound in another set should also select that set directly from the timeline
- the active set determines which sounds the keyboard appends to
- the active set determines which sounds can be dragged
- boundaries are visual indicators of where sets begin
- sets do not own independent hidden spacing anymore beyond the per-sound spacing that already exists

## Keyboard Rule

- keyboard is attached directly below the grid
- keyboard and grid share horizontal pitch scroll
- the keyboard should never bring back the old bottom dock architecture
- leave a small safe-space above the home indicator

## Explicit V1 Non-Goals

Do not add these unless there is a strong reason:

- per-sound duration editing
- velocity / strike intensity editing
- articulation presets
- a full DAW-style resize-and-envelope workflow
- hidden special timing rules for cues

## Future Reinference Hooks

The editor is still expected to become the reinference surface later.

Likely future additions:

- inferred vs confirmed set state in the sets overlay
- lock/unlock set controls
- infer-missing-sets action
- stronger preview of inferred boundaries and grouped sounds
