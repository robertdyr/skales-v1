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
```

## Shared UI Principles

- make the library the calm home screen
- keep one primary action per screen
- keep playback actions obvious and fast to reach
- keep analyzer evidence readable, not hidden
- keep correction simpler than inference internals

## Shared Layout Principles

- mobile-first vertical layouts
- primary action near the bottom or clearly separated
- result summaries above detailed evidence
- empty/loading/error states documented explicitly
- cards/sections should visually separate tasks: summary, controls, evidence, save action

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
