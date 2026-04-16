# Editor Screen

## Purpose

The editor is the correction, authoring, and reinference workspace.

It should feel functional, structured, and easy to understand even when a scale has multiple sets.

## Layout

```text
+---------------- top bar ----------------+
| New scale / Edit scale                  |
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
| pitch rows + time grid                  |
| draggable note blocks                   |
| snap: 1/1, 1/2 default, 1/4 optional   |
| changing snap does not rewrite timing   |
| selected-set editing only               |
+----------------------------------------+

+-------------- keyboard -----------------+
| piano keyboard / armed note             |
+----------------------------------------+

+---------------- actions ----------------+
| infer missing / lock confirmed sets     |
| clear selected set / save               |
+----------------------------------------+

+---------- sticky playback dock ---------+
| tabs: Playback | Keyboard              |
| Playback: cursor + bpm +/- + transport |
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

## Set Editing Rule

Use one piano roll for the active set.

- dragging a note edits only the selected set
- other sets appear as compact previews, not as editable overlapping lanes
- moving content across sets should require an explicit action, not a normal drag
- changing snap changes the grid, not the saved timing
- set cards in the lower section should be fully tappable for selection

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
