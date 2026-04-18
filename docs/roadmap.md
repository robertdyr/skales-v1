# Roadmap

## Goal

Build the app around the fastest useful creation loop first:

```text
editor -> infer -> correct -> save -> play
```

The full product vision includes recording analysis, but the MVP focuses on editor-driven creation and inference first.

`overview.md` describes the full product vision.
This roadmap describes what gets built first and in what order.

## Current Priority

Focus first on making new scales easy to create without audio analysis.

That means:

- fast manual editing in the editor
- active-set piano-roll editing with configurable snap
- partial-set inference
- reinference with locked corrected sets
- smooth save and playback handoff

Important model note:

- the editor and infer roadmap now assumes a likely migration toward absolute step-position event storage
- grouped sounds and inference merge should be built with that direction in mind

Why this comes first:

- recording analysis is harder
- recording analysis depends on a good correction target anyway
- the editor + infer loop is useful even without any recording features

This is the execution order for the MVP and early follow-up phases.

## Component Priority Order

1. `editor`
2. `infer`
3. `player`
4. `storage`
5. `analyzer`
6. `app-shell`

That order is about active product focus, not importance in the overall architecture.

## Phases

### Phase 1

Ship the MVP editor-first creation workflow.

- create scales manually
- place and drag notes in a piano roll
- support non-destructive snap changes: 1/1, 1/2, 1/4
- keep one active set in focus while other sets remain visible in the shared timeline
- seed one or more sets
- infer the remaining sets
- save the result
- play the saved scale

### Phase 2

Make reinference practical and pleasant.

- finalize grouped simultaneous sounds well enough for cue chords
- migrate timing/storage toward stable internal step positions
- lock corrected sets
- reinfer unresolved sets
- keep inferred vs confirmed sets visually clear
- keep playback preview close to editing
- add explicit quantize actions only if needed, separate from snap switching

### Phase 3

Improve inference quality.

- probe suggestions for a few sets before wider generation
- fill-range inference around confirmed anchors
- better ranking from partial evidence
- better naming suggestions

### Phase 4

Add recording analysis on top of the editor + infer workflow.

- imported audio -> evidence
- evidence -> infer draft
- correction in editor
- reinference from corrected sets

### Phase 5

Add live recording later.

- microphone capture
- same downstream analyzer -> infer -> editor flow

## Target Flow

```mermaid
flowchart LR
    Library["Library"] --> Editor["Editor"]
    Editor --> Seed["Enter known sets"]
    Seed --> Infer["infer"]
    Infer --> Correct["Review and correct proposal"]
    Correct -. lock sets and infer again .-> Infer
    Correct --> Save["Save"]
    Save --> Player["Player"]
```

## Future Agent Note

If you are picking up work after this roadmap update, read the timing-model notes first.

The likely order of architectural work is:

1. settle stable internal step resolution
2. migrate saved timing toward absolute step positions
3. support grouped simultaneous sounds cleanly
4. implement editor confirmed vs suggested set state
5. implement probe suggestions and fill-range inference on top of that

Avoid building deep new inference or grouped-sound logic on top of assumptions that `breakAfterBeats` is the final long-term storage truth.

## Later Flow

```mermaid
flowchart LR
    Import["Import/Record audio"] --> Analyzer["analyzer"]
    Analyzer --> Evidence["note evidence"]
    Evidence --> Infer["infer"]
    Infer --> Editor["Editor"]
    Editor --> Save["Save"]
```

## Decision Rule

If a new task appears, ask this first:

1. does it make editor-first scale creation easier?
2. does it improve infer quality or reinference usability?
3. is it recording-analysis work that can wait until the editor loop is solid?

If it is mostly about recording analysis, it is probably later-roadmap work for now.
