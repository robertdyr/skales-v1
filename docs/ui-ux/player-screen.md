# Player Screen

## Purpose

The player is the focused practice screen.

It should feel clean, readable, and immediate.

## Layout

```text
+---------------- top bar ----------------+
| Back         Scale name            Edit |
+----------------------------------------+

+--------------- settings ----------------+
| tempo                                  |
| direction                              |
+----------------------------------------+

+-------------- current set --------------+
| set number                             |
| sound circles / note labels            |
+----------------------------------------+

+--------------- controls ----------------+
| Play   Stop                <- Step ->   |
+----------------------------------------+
```

## UX Priorities

1. current playback position should be obvious
2. play and step should be easy to reach
3. direction should be understandable without explanation
4. edit handoff should stay available but secondary

## State Handling

### Loading

- simple centered loading state

### Missing scale

- simple not-found state

### Ready

- controls and current set visible immediately

## Future UX Additions

- stronger visual highlight of current note/sound
- waveform-like rhythm/timing preview if richer timing is added
- optional compact practice mode
