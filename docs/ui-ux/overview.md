# UI / UX Overview

## Purpose

This folder documents how the app should look and behave on screen.

Architecture docs explain ownership.
These docs explain layout, emphasis, interaction flow, and state presentation.

## Screen Set

```text
Library
  -> Import Review Flow
  -> Editor
  -> Player

Cross-screen loop:
  Editor -> infer -> Editor
```

## Shared UI Principles

- make the library the calm home screen
- keep one primary action per screen
- keep playback actions obvious and fast to reach
- keep analyzer evidence readable, not hidden
- keep inferred results editable and reinferable
- keep correction simpler than inference internals
- for multi-set editing, prefer one shared timeline with explicit set boundaries over isolated per-set piano rolls
- treat snap size as a safe editor tool, not as an implicit quantize command
- keep editor controls as floating overlays instead of restoring the old bottom dock
- keep the keyboard attached directly to the piano roll
- do not auto-jump the editor viewport on every edit
- keep editor save as a clearly separated top-bar action rather than a repeated bottom footer

## Shared Layout Principles

- mobile-first vertical layouts
- primary action near the bottom or clearly separated; app-bar actions are preferred when they reduce long-scroll friction
- result summaries above detailed evidence
- locked user-confirmed sets should remain visually distinct from inferred sets
- empty/loading/error states documented explicitly
- when the editor is the main task, let the piano roll own most of the screen instead of wrapping it in extra cards

## Shared Flow Rule

When a screen deals with imported or inferred content, keep the handoff explicit:

```text
audio -> evidence -> inferred draft -> review/edit -> save
manual seed -> inferred draft -> correct -> reinfer -> save
```

## Shared Visual Rule

Use box-like sections consistently:

```text
+---------------- summary ----------------+
| key result / title / source            |
+----------------------------------------+

+---------------- controls ---------------+
| primary actions / tuning controls      |
+----------------------------------------+

+---------------- details ----------------+
| notes / candidates / debug evidence    |
+----------------------------------------+
```
