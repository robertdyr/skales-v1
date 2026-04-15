# Implementation Roadmap

## Goal

Build the app as a set of clear internal libraries, then wire them together with the Android shell.

Primary components:

- `analyzer`
- `player`
- `editor`
- `storage`
- `app-shell`

## Component Roadmap

```text
Phase 1: app-shell + analyzer + storage + player
         file import -> draft -> save -> play

Phase 2: analyzer + editor
         draft correction flow

Phase 3: analyzer input
         microphone recording

Phase 4: analyzer contextualizer
         richer timing and musical interpretation
```

## What Each Box Should Become

### `analyzer`

Target contract:

```text
audio input -> ScaleDraft + evidence
```

Internal sub-libraries:

- `input`
- `recognizer`
- `contextualizer`
- `draft-builder`

Recommended future types:

- `Analyzer`
- `NoteExtractor`
- `ScaleContextualizer`
- `TimingInferencer`

### `player`

Target contract:

```text
Scale -> playback
```

Already mostly there.

### `editor`

Target contract:

```text
draft or manual input -> Scale
```

Already mostly there, but should learn how to open from `ScaleDraft`.

### `storage`

Target contract:

```text
Scale <-> persistence
```

Already there.

### `app-shell`

Target contract:

```text
user flow orchestration
```

This should stay thin.

## Immediate Next Refactor

The current code works, but the `analyzer` component is still spread across pipeline-stage classes.

Best next architectural cleanup:

1. introduce `NoteExtractionResult`
2. introduce `NoteExtractor`
3. introduce `ScaleContextualizer`
4. introduce `TimingInferencer`
5. wrap them behind one top-level `Analyzer`

That gets the code closer to the mental model:

```text
file -> analyzer -> draft -> player
```

instead of:

```text
file -> many small pipeline classes the app knows about
```

## Phased Build Plan

### Phase 1

Done or mostly done:

- import file
- analyze file
- review draft
- save draft
- play saved scale

### Phase 2

Build:

- open review result in editor
- keep analyzer evidence visible while correcting
- improve scale naming before save

### Phase 3

Build:

- microphone input library
- recorder UI in app-shell
- same analyzer downstream path

### Phase 4

Build inside `contextualizer`:

- timing inference between sounds
- timing inference between sets
- improved scale grouping
- optional LLM reasoning over structured note evidence

### Phase 5

Refine `player` and `editor` to support richer timing authored by the analyzer.

## Visual Future State

```text
         +-------------------+
         |     app-shell     |
         +-----+--------+----+
               |        |
               v        v
        +----------+  +--------+
        | analyzer |  | editor |
        +-----+----+  +----+---+
              |            |
              v            v
             +--------------+
             |   storage    |
             +------+-------+
                    |
                    v
                +-------+
                |player |
                +-------+
```

## Decision Rule

If a new feature idea appears, first ask which component it belongs to.

Examples:

- mp3 loading: `analyzer.input`
- pitch recognition: `analyzer.recognizer`
- LLM interpretation: `analyzer.contextualizer`
- break timing inference: `analyzer.contextualizer`
- tempo-controlled playback: `player`
- draft correction UI: `editor`
- navigation between them: `app-shell`
