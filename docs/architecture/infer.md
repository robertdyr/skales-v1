# Infer

## Responsibility

`infer` turns incomplete musical evidence into a reviewable proposal.

That evidence can come from either:

- the analyzer after audio recognition
- the editor after the user authors one or more trusted sets

The component supports the iterative workflow:

1. user enters one or more sets
2. engine suggests a likely continuation
3. user corrects or confirms some sets
4. engine suggests again with those confirmed anchors preserved

## Core Rule

Inference is not the same thing as analysis.

- `analyzer` extracts note evidence from audio
- `infer` proposes the larger exercise structure from partial evidence

That split matters because editor-assisted inference must work even when there is no recording at all.

## Editor Inference Direction

Editor-driven inference should be understood as proposal-oriented, not overwrite-oriented.

That means:

- confirmed sets are trusted anchors
- suggested sets are provisional
- the editor remains the review and correction surface
- inference should integrate into the working sequence rather than opening a separate draft editor for normal correction

## Confirmed Anchors

The important conceptual shift is this:

- confirmed sets are not necessarily the beginning of the full exercise

They may sit:

- at the start
- in the middle
- near the top of the intended range
- as several anchors with missing regions between them

So inference must support generating:

- before confirmed anchors
- after confirmed anchors
- around confirmed anchors
- eventually between confirmed anchors if needed

## Probe vs Fill Range

Two editor-facing actions are expected to be useful.

### Probe suggestion

Purpose:

- test whether the engine understood the pattern
- keep review low-overwhelm

Typical behavior:

- suggest 1 or 2 sets
- keep them provisional
- let the user accept, reject, or edit them

### Fill range

Purpose:

- generate a larger span once the pattern looks right

Typical behavior:

- fill a requested pitch range
- possibly generate before the currently confirmed region
- possibly generate after the currently confirmed region
- return a larger proposal for review

`Fill range` should not be modeled as append-only.

## Recommended Interface Direction

The exact Kotlin classes may evolve, but the editor-facing request should support these ideas:

- current ordered working sets
- which sets are confirmed anchors
- requested mode such as probe or fill-range
- optional pitch bounds
- optional count limit as a safety bound
- optional name or prompt hint

Conceptually:

```kotlin
interface ScaleInferEngine {
    fun infer(request: ScaleInferenceRequest): ScaleInferenceResult
}
```

## Result Direction

For editor use, the important output is usually not a silent replacement draft.

It is a reviewable proposal the editor can merge into the working sequence.

Good result concepts:

- proposed ordered sets for the requested scope
- optional candidate ranking metadata
- optional name suggestion

For an LLM-backed inference engine, returning a full proposed ordered sequence for the requested scope may be simpler and more robust than returning only append operations or surgical insertion instructions.

That is especially true once requested range may extend below already confirmed sets.

## Merge Rules

The editor merge step should follow these rules:

- confirmed sets are never silently replaced
- suggested sets may be regenerated
- accepting a suggestion promotes it to confirmed
- directly editing a suggested set promotes it to confirmed
- rejecting a suggestion removes it from the working sequence
- inference may insert suggested sets before existing confirmed sets if the requested scope requires it

This keeps the working model simple:

- one ordered working sequence in the editor
- per-set review state layered on top
- one final saved `Scale` once the user is satisfied

## Why Step-Based Timing Helps Inference

Inference is easier to reason about when the underlying model is structurally step-based.

Why:

- the engine can think in ordered event positions
- grouped simultaneous sounds are just several notes in one event at one step
- range fill becomes a problem of proposing ordered events within bounds
- editor merge becomes simpler because proposed positions are structural, not reconstructed through relative gaps

This is one of the main reasons the architecture is leaning away from `breakAfterBeats` as the long-term storage truth.

## Analyzer-Facing Inference

The analyzer path may still want a draft-like whole-result handoff.

That is fine.

But editor-facing inference should be optimized for:

- incremental suggestions
- local review
- reinference with trusted anchors

The two use cases do not need to force the exact same result shape if that would harm clarity.

## What Infer Owns

- candidate scale ranking
- proposal generation from partial evidence
- preserving confirmed anchors during reinference
- name suggestion based on the best candidate

## What Infer Must Not Own

- audio decoding
- pitch detection
- editor UI state
- persistence
- review state transitions such as accept, reject, or confirm

Those stay in the editor layer.

## Migration Note For Future Agent

The current code in `infer/` still reflects an older whole-draft replacement mindset and a simpler interval-template implementation.

Do not assume that is the target shape.

Current documentation direction:

- editor-facing infer should return proposals suitable for merge
- confirmed anchors may exist anywhere in the larger exercise
- step-based event placement is the preferred long-term structural model

If implementing inference next:

1. finalize the editor-side confirmed vs suggested set state
2. decide the stable internal step resolution for saved timing
3. define an editor-focused inference request/result contract around proposals
4. only then implement probe suggestions and larger range fill

## Current Direction

Initial implementation can still be heuristic or LLM-backed.

The important architectural move is separating:

- extraction of evidence
- proposal generation from evidence
- editor-side review and merge

That separation keeps future improvements localized.
