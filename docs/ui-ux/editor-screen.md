# Editor Screen

## Purpose

The editor is the main surface for creating and correcting a scale.

The piano roll and keyboard are the primary UI. Playback, grid controls, and set controls support that surface.

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
2. keep set structure visible
3. make timing edits direct and readable
4. keep same-step simultaneous notes understandable
5. avoid DAW-style complexity

## Interaction Model

- all sets are visible on one grid
- one set is selected for editing ownership
- tapping a sound in another set selects that set
- keyboard input adds sounds to the selected set

## Timing Behavior

- the saved model is absolute-step based
- each sound stores its own `step`
- changing snap does not rewrite saved positions
- snap only affects placement and drag quantization

## Set Boundaries

- a set starts where its first sound starts
- the separator line reflects that start
- dragging the separator moves that set start explicitly
- dragging the first sound of a later set also moves that set start
- dragging later sounds in that set does not move the separator

## Sound Rendering

- note sound: square block
- cue sound: circular block
- simultaneous notes: multiple independent items at one time position
- vertical drag transposes the dragged sound only

## Controls

### Playback

- playback lives in a floating overlay
- the main action is play and stop
- playback should match the timing shown on the grid

### Grid

- snap options are `1/1`, `1/2`, and `1/4`
- `1/2` is the default
- snap affects placement and drag quantization only

### Sets

- the sets overlay selects the active set
- it exposes set-level actions such as add, delete, and clear
- sets remain real structure even though editing happens on one shared grid

## Non-Goals

- per-sound duration editing
- velocity or articulation editing
- DAW-style resizing tools

## Future Work

- inferred vs confirmed set state
- lock and unlock controls
- probe and fill-range inference entry points
- better local playback review
