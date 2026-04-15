# App Shell

## Responsibility

The app shell wires the internal libraries into a usable Android application.

## External Contract

```text
user actions <-> app flow orchestration
```

## Internal Shape

```text
+-------------------- app-shell --------------------+
|                                                   |
|  screens + nav + viewmodels + permissions + DI    |
|                                                   |
+---------------------------------------------------+
```

## Current Code Mapping

- `MainActivity`
- `SkalesApplication`
- `ui/navigation/NavGraph.kt`
- screen-specific view models

## Responsibilities

- navigation between library, editor, import, review, and player
- dependency construction and wiring
- file picker integration
- permission and lifecycle handling

## What App Shell Should Avoid

- embedding analyzer logic directly in screens
- embedding playback logic directly in screens
- storing business rules that belong in component libraries
