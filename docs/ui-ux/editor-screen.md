# Editor Screen

## Purpose

The editor screen is the main surface for building and correcting a scale.

The piano roll and keyboard are the primary UI. Playback, grid controls, and set controls support that surface and should stay secondary.

## Layout

```text
+---------------- top bar ----------------+
| Back   New scale / Edit scale    Save   |
+----------------------------------------+

+--------------- metadata ----------------+
| scale name                             |
+----------------------------------------+

+------------- shared timeline -----------+
| full-height piano roll                  |
| all sets on one grid                    |
| time runs upward                        |
| pitch runs left-right                   |
| floating controls: play / grid / sets   |
+----------------------------------------+

+--------------- keyboard ----------------+
| attached directly under the grid        |
| shares horizontal pitch scroll          |
+----------------------------------------+
```

## UX Priorities

1. keep the piano roll and keyboard dominant
2. keep set structure visible without splitting editing into separate panes
3. make timing edits readable and direct
4. keep cues and grouped sounds editable as real events
5. avoid DAW-style complexity

## Interaction Model

The screen shows one shared timeline for the entire scale.

- all sets are visible on the same grid
- the selected set is highlighted and editable
- other sets stay visible as context
- tapping a sound in another set selects that set
- keyboard input appends to the selected set

## Set Boundaries

Set boundaries are visible grouping markers, not separate timing objects.

- a set starts where its first sound starts
- the separator line reflects that start
- dragging the separator moves that set start explicitly
- dragging the first sound of a later set also moves that set start
- dragging later sounds in that set should not move the separator

## Timing Behavior

For v1, only one timing value is exposed:

- `breakAfterBeats`

This means:

- the visible gap after a sound is owned by that sound
- the gap across a set boundary is still owned by the earlier sound
- moving a sound rewrites adjacent spacing based on the new shared timeline order

Expected editing behavior:

- dragging a later sound within a set changes spacing inside that set
- dragging the first sound of a later set changes both the set start and the previous cross-set gap
- dragging a separator changes the same boundary explicitly
- changing grid snap affects placement and dragging only, not saved timing globally

## Sound Rendering

- note sound: square block
- cue sound: circular block
- grouped cue: multiple circles at one time position
- multi-note sound: one event that moves together
- vertical drag transposes the whole sound together

## Controls

### Playback

- playback lives in a floating overlay, not a dock
- the main action is play and stop
- playback should match the timing the user sees in the grid

### Grid

- snap options are `1/1`, `1/2`, and `1/4`
- `1/2` is the default
- snap affects placement and drag quantization only

### Sets

- the sets overlay selects the active set
- it also exposes set-level actions such as add, delete, clear, and cue actions
- sets remain real structure even though editing happens on one shared grid

## Non-Goals

Do not add by default:

- per-sound duration editing
- velocity or articulation editing
- full DAW-style resizing tools
- hidden timing rules for cues

## Future Hooks

Later reinference work will likely add:

- inferred vs confirmed set state
- lock and unlock controls
- infer-missing-sets actions
- better draft review support inside the editor
