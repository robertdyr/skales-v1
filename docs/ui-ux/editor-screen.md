# Editor Screen

## Purpose

The editor is the correction and authoring workspace.

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

+-------------- keyboard -----------------+
| piano keyboard                          |
+----------------------------------------+

+---------------- sets -------------------+
| new set / add cue / delete set          |
| set cards                               |
| remove last / clear selected            |
+----------------------------------------+

+--------------- footer ------------------+
| Cancel                     Save         |
+----------------------------------------+
```

## UX Priorities

1. always show which set is selected
2. keep note entry fast
3. let preview playback happen without leaving the editor
4. make save eligibility obvious

## Future Draft-Correction Mode

When opened from analyzer output, the editor should additionally show:

```text
+------------ imported evidence ----------+
| draft source / candidate / note summary |
+----------------------------------------+
```

This should remain supportive, not overpower the editing workspace.
