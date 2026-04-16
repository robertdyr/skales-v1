# Editor Screen

## Purpose

The editor is the correction, authoring, and reinference workspace.

It should feel functional, structured, and easy to understand even when a scale has multiple sets.

## Layout

```text
+---------------- top bar ----------------+
| Back   New scale / Edit scale    Save   |
+----------------------------------------+

+--------------- metadata ----------------+
| scale name                              |
+----------------------------------------+

+---------------- set strip --------------+
| Set 1 | Set 2 | Set 3 | ...             |
| one active set selected                 |
| new set / cue / delete near strip       |
+----------------------------------------+

+-------------- piano roll ---------------+
| fixed-height viewport + time grid       |
| draggable note blocks                   |
| snap: 1/1, 1/2 default, 1/4 optional   |
| changing snap does not rewrite timing   |
| vertical scroll for more octaves        |
| selected-set editing only               |
+----------------------------------------+

+---------------- set details ------------+
| selected set preview / clear selected   |
+----------------------------------------+

+---------- sticky playback dock ---------+
| tabs: Playback | Keyboard              |
| Playback: cursor + bpm +/- + compact   |
| transport controls                     |
| Keyboard: sticky keyboard note entry   |
| attached to bottom, not floating card  |
+----------------------------------------+
```

## UX Priorities

1. always show which set is selected
2. keep note spacing visible and editable
3. keep note entry fast
4. keep playback controls reachable while editing and scrolling
5. keep keyboard note entry reachable while editing and scrolling
6. avoid accidental cross-set edits
7. make snap changes safe and predictable
8. keep save reachable without forcing a scroll to the bottom

## Set Editing Rule

Use one piano roll for the active set.

- dragging a note edits only the selected set
- other sets appear as compact previews, not as editable overlapping lanes
- moving content across sets should require an explicit action, not a normal drag
- changing snap changes the grid, not the saved timing
- set cards in the lower section should be fully tappable for selection

## Piano Roll Viewport Rule

Keep the piano roll vertically bounded on screen while still allowing the full pitch range.

- use a fixed-height viewport instead of rendering the full pitch range at once
- allow vertical scrolling for more octaves rather than clipping to a narrow hard-coded range
- auto-position the viewport near the selected set's notes when the active content changes
- avoid trapping note drags at arbitrary visible octave edges

## Save Action Rule

Save is a top-bar action, not a bottom form footer.

- keep `Save` in the app bar opposite `Back`
- disable save until the scale has a name and at least one playable note
- avoid a duplicate bottom save/cancel panel in the editor

## Playback Panel Rule

The playback panel in the editor dock should stay compact and playback-first.

- use an icon-only primary play/stop control
- show stop only while playing by swapping the primary control state
- keep secondary playback actions like `Step` and `Reset` lighter than the primary control
- preserve breathing room through layout balance before reducing dock padding

## Snap Rule

Snap is an editor control, not a destructive transform.

- `1/2` is the default snap
- `1/1` and `1/4` are optional views/editing snaps
- switching snap should only affect future placement and dragging
- rewriting existing timing belongs to a separate quantize action if added later

## Future Draft-Correction And Reinference Mode

When opened from analyzer output or when running a seeded reinference loop, the editor should additionally show:

```text
+------------ imported evidence ----------+
| draft source / candidate / note summary |
+----------------------------------------+

+------------ inference controls ---------+
| infer missing sets                      |
| lock/unlock corrected sets              |
+----------------------------------------+
```

This should remain supportive, not overpower the editing workspace.
