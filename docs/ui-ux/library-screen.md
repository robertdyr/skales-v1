# Library Screen

## Purpose

The library is the home screen.

It should feel simple, calm, and decisive.

## Layout

```text
+---------------- top bar ----------------+
| Skales                  Import   Debug  |
+----------------------------------------+

+---------------- content ----------------+
| saved scale cards / empty state        |
|                                        |
| [Scale card]                           |
| [Scale card]                           |
| [Scale card]                           |
+----------------------------------------+

                    [ New ]
```

## Visual Priorities

1. existing saved scales
2. create new scale
3. import from audio
4. continue an inference/correction flow when present
5. debug access should remain secondary

## Empty State

```text
+--------------- empty state -------------+
| No saved scales yet                    |
| Create manually or import from audio   |
| [Create first scale]                   |
| [Import from audio]                    |
+----------------------------------------+
```

## Interaction Rules

- tapping a card opens the player
- edit stays available but secondary
- delete requires confirmation
- import should be visible without competing with the main list
- if draft or reinference flows are added later, the handoff into review/editor should feel like a continuation of creation, not a separate tool
