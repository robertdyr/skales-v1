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
| scale name field only                   |
+----------------------------------------+

+-------------- piano roll ---------------+
| fixed-height viewport + time grid       |
| draggable note blocks                   |
| snap: 1/1, 1/2 default, 1/4 optional   |
| snap row includes delete icon          |
| changing snap does not rewrite timing   |
| vertical scroll for more octaves        |
| horizontal focus follows newest note    |
| selected-set editing only               |
+----------------------------------------+

+---------- sticky playback dock ---------+
| tabs: Playback | Keyboard | Sets       |
| Playback: cursor + bpm +/- + compact   |
| transport controls                     |
| Keyboard: append-at-end note entry     |
| + quick new-set action                 |
| Sets: active set strip + selected set  |
| detail card + pre/post cue actions     |
| attached to bottom, not floating card  |
+----------------------------------------+
```

## UX Priorities

1. always show which set is selected
2. keep note spacing visible and editable
3. keep note entry fast
4. keep playback controls reachable while editing and scrolling
5. keep keyboard and set controls reachable while editing and scrolling
6. avoid accidental cross-set edits
7. make snap changes safe and predictable
8. keep save reachable without forcing a scroll to the bottom

## Set Editing Rule

Use one piano roll for the active set.

- tapping empty piano-roll space places a note at the tapped pitch and time column
- keyboard entry appends a note at the end of the selected set
- dragging a note edits only the selected set
- other sets appear as compact previews, not as editable overlapping lanes
- moving content across sets should require an explicit action, not a normal drag
- changing snap changes the grid, not the saved timing
- set selection and set actions live in the dock rather than duplicating set controls in the main scroll content
- support both pre-cues and post-cues for scale exercises that announce the target both before and after the note run

## Piano Roll Viewport Rule

Keep the piano roll vertically bounded on screen while still allowing the full pitch range.

- use a fixed-height viewport instead of rendering the full pitch range at once
- allow vertical scrolling for more octaves rather than clipping to a narrow hard-coded range
- auto-position the viewport near the selected set's notes when the active content changes
- horizontally bias the viewport toward the newest note column so recent edits stay near center
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

## Sets Panel Rule

Set management belongs in the editor dock beside playback and keyboard.

- provide one dedicated `Sets` tab in the dock rather than duplicate set sections in the main content
- keep the selected set obvious from the highlighted set strip; do not duplicate selection state with a second badge inside the detail card
- keep `New set`, cue actions, delete, and clear close to set selection
- distinguish pre-cue and post-cue actions explicitly instead of treating cue placement as one generic action
- let the piano roll remain the center of the screen while sets work alongside it
- keep the selected-set detail card at a stable height so the dock does not jump when switching between empty and non-empty sets
- allow long note/cue summaries and action rows to scroll horizontally rather than clipping

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
