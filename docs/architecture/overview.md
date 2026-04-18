# Architecture Overview

## Main Components

- `app-shell`: screens, navigation, viewmodels, orchestration
- `editor`: editing operations and grid projection
- `player`: playback stepping and sound output
- `infer`: proposal generation from partial information
- `analyzer`: audio-to-evidence pipeline
- `storage`: Room persistence and repository
- `model`: shared domain models

## Dependency Direction

`app-shell` depends on the feature components and coordinates between them.

Components should not depend on Compose UI code or on each other's screen state.

## Ownership Summary

- `model`: shared domain types
- `editor`: transforms and edits `Scale`
- `player`: plays `Scale`
- `infer`: proposes `ScaleSet`s from partial information
- `analyzer`: extracts note evidence
- `storage`: saves and loads approved `Scale`s
- `app-shell`: wires everything together

## Key Rule

Keep components narrow and testable.

- screen/session state belongs in viewmodels
- saved domain state belongs in `model`
- persistence concerns belong in `storage`
