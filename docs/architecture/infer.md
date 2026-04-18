# Infer

## Responsibility

`infer` turns incomplete musical evidence into a reviewable proposal.

That evidence can come from:

- analyzer output
- user-authored sets in the editor

## Core Rule

Inference is separate from analysis.

- `analyzer` extracts note evidence
- `infer` proposes larger exercise structure from partial evidence

## Editor-Facing Direction

Inference should be proposal-oriented.

That means:

- confirmed sets act as anchors
- suggested sets remain provisional
- inference integrates into the same working editor sequence
- inference should not replace confirmed work silently

## Range Behavior

Confirmed sets are not necessarily the beginning of the exercise.

Inference must support generating:

- after confirmed sets
- before confirmed sets
- eventually around gaps between confirmed sets

## Request Shape

Editor-facing inference requests should support:

- current ordered sets
- which sets are confirmed anchors
- requested mode such as probe or fill-range
- optional pitch bounds or count bounds

## Result Shape

Inference results should be easy for the editor to merge.

Useful output:

- proposed ordered sets
- optional ranking metadata
- optional name suggestion

## Why The Step Model Helps

Step-based timing keeps inference simpler because:

- events have stable structural positions
- simultaneous notes are just several sounds at one step
- merge logic works on positions instead of reconstructed gaps

## What Infer Must Not Own

- audio decoding
- pitch detection
- editor UI state
- persistence
- review state transitions

## Current Direction

Future infer work should focus on:

- confirmed vs suggested set flow
- probe suggestions
- fill-range suggestions
- better proposal quality and naming
