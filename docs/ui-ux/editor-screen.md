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

+--------------- playback ----------------+
| tempo controls                          |
| step / play / stop / reset              |
| cursor status                           |
+----------------------------------------+

+---------------- set strip --------------+
| Set 1 | Set 2 | Set 3 | ...             |
| one active set selected                 |
+----------------------------------------+

+-------------- piano roll ---------------+
| pitch rows + time grid                  |
| draggable note blocks                   |
| selected-set editing only               |
+----------------------------------------+

+-------------- keyboard -----------------+
| piano keyboard / armed note             |
+----------------------------------------+

+---------------- actions ----------------+
| new set / add cue / delete set          |
| infer missing / lock confirmed sets     |
| clear selected set / save               |
+----------------------------------------+

+--------------- footer ------------------+
| Cancel                     Save         |
+----------------------------------------+
```

## UX Priorities

1. always show which set is selected
2. keep note spacing visible and editable
3. keep note entry fast
4. let preview playback happen without leaving the editor
5. make reinference available without overwhelming manual editing
6. avoid accidental cross-set edits

## Set Editing Rule

Use one piano roll for the active set.

- dragging a note edits only the selected set
- other sets appear as compact previews, not as editable overlapping lanes
- moving content across sets should require an explicit action, not a normal drag

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
