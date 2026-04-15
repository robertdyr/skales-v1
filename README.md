# Skales

Skales is an Android app for creating, inferring, saving, and replaying custom singing-practice scales.

## Documentation

Full documentation is available in `/docs`:

- **`docs/overview.md`** - product vision, current build focus, and core user flows
- **`docs/roadmap.md`** - development phases and priorities
- **`docs/architecture/`** - component architecture and design
- **`docs/ui-ux/`** - screen layout and interaction design
- **`docs/implementation/`** - execution notes

Start with `docs/overview.md` for the big picture, then explore component-specific docs in `docs/architecture/`.

## Quick Summary

### Product Vision

Recording-first scale creation: record or import audio → analyze → infer draft → correct → save → play.

### Current Build Focus

Editor-first scale authoring with partial-set inference:

```text
editor → seed sets → infer missing sets → correct → save → play
```

The MVP focuses on manual editing and inference before adding full recording analysis.

### Stack

- Kotlin
- Jetpack Compose + Material3
- MVVM architecture
- Coroutines + StateFlow
- Room for local persistence
- Navigation Compose
- Sample-based piano playback via `SoundPool`

### Core Components

- **`analyzer`** - audio → note evidence (future)
- **`infer`** - seed/evidence → draft generation
- **`editor`** - scale authoring and correction
- **`player`** - scale playback
- **`storage`** - Room persistence
- **`app-shell`** - screens, navigation, ViewModels

See `docs/architecture/overview.md` for the full component architecture and `docs/architecture/models.md` for the domain model details.

## Project Status

**Implemented:**
- Scale library screen
- Manual editor with piano-roll editing
- Scale player with step and autoplay
- Local persistence with Room
- Basic inference support

**In Progress:**
- Locked-set reinference flow
- Richer timing inference
- Playback preview in editor

**Future:**
- Recording analysis (analyzer component)
- Microphone capture workflow

See `docs/roadmap.md` for detailed phases and priorities.
