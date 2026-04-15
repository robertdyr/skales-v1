# Editor

## Responsibility

The editor is the **fallback** surface for creating or correcting a `Scale`.

The primary path is recording → auto-recognition. The editor serves two roles:
1. **Correction**: fix mistakes in an auto-recognized scale
2. **Manual creation**: for power users who want full control

Ideally, recording works so well that most users never need the editor. But it must exist as a safety net and for users who prefer manual input.

## External Contract

```text
Input  : manual note entry, and later draft-to-edit handoff
Output : final Scale
```

## Internal Shape

```text
+--------------------- editor ---------------------+
|                                                   |
|  input controls -> editable state -> saveable     |
|                     |               Scale         |
|                     +-> preview playback          |
|                                                   |
+---------------------------------------------------+
```

## Current Responsibilities

- name the scale
- manage sets
- append notes into the selected set
- add or remove cue sounds
- preview playback
- save the final scale

## Current Code Mapping

- `app/screens/ScaleEditorScreen`
- `app/viewmodel/ScaleEditorViewModel`
- `app/components/PianoKeyboard`

Note: Currently the editor logic lives in the app shell. A future `editor/` package could extract reusable editing state management if needed.

## Future Role In Audio Flow

The editor should become the correction surface after analysis.

Target handoff:

```text
ScaleDraft -> Editor -> corrected Scale
```

## What The Editor Must Not Know

- how pitch detection works
- how files are imported
- how candidate ranking is computed

It should accept draft-like data, not own analysis logic.
