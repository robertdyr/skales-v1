# Architecture Overview

## Mental Model

Think of Skales as an Android shell around a few internal libraries.

Primary boxes:

- `analyzer`
- `player`
- `editor`
- `storage`
- `app-shell`

## Box Diagram

```text
            +-------------------+
            |     app-shell     |
            | screens, nav, VM  |
            +---------+---------+
                      |
      +---------------+----------------+
      |               |                |
      v               v                v
+-----------+   +-----------+   +-------------+
| analyzer  |   |  editor   |   |   player    |
| audio ->  |   | draft ->  |   | Scale ->    |
| ScaleDraft|   | Scale     |   | sound       |
+-----+-----+   +-----+-----+   +------+------+
      |               |                |
      +---------------+----------------+
                      |
                      v
               +-------------+
               |   storage   |
               | Room / repo |
               +-------------+
```

## Ownership Summary

```text
analyzer  : audio -> draft + evidence
editor    : editable draft/state -> Scale
player    : Scale -> audible playback
storage   : Scale <-> persistence
app-shell : user flow orchestration
```

## Current Code Mapping

```text
model/     -> shared domain models (Scale, Note, etc.)
analyzer/  -> (not yet implemented) audio -> draft + evidence
player/    -> PianoSoundPlayer, ScaleAutoPlayer, ScaleStepper
storage/   -> local/ (Room DB), ScaleRepository
app/       -> MainActivity, SkalesApplication, screens/, viewmodel/, navigation/, components/, theme/
```

The boxes are packages within `com.example.skales`:

```text
com.example.skales/
├── model/      # shared models used by all boxes
├── analyzer/   # (future) audio recognition
├── player/     # playback engine
├── storage/    # persistence
└── app/        # shell: screens, viewmodels, navigation, UI
```

## Dependency Direction

The intended dependency flow is:

```text
app-shell
  -> analyzer
  -> editor
  -> player
  -> storage
```

And not the other way around.

Component rules:

- `player` should not know about raw analysis models
- `storage` should persist final approved scales, not arbitrary raw analysis state
- `editor` should not own pitch detection logic
- `analyzer` should not depend on Compose UI code

## Document Map

- `analyzer.md`: audio-to-draft library
- `player.md`: playback library
- `editor.md`: manual editing library
- `storage.md`: persistence library
- `app-shell.md`: Android orchestration layer
