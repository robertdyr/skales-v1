# Import Review Flow

## Purpose

This flow takes the user from audio file to note evidence, an inferred draft, and then a reviewable scale.

## Flow Shape

```text
Import audio -> Analyze audio -> Infer draft -> Review -> Save
                                             \-> Edit -> Reinfer -> Review
```

## Import Screen Layout

```text
+---------------- top bar ----------------+
| Back                 Import audio       |
+----------------------------------------+

+--------------- import card -------------+
| choose audio file                       |
| selected file                           |
| loading / analyzing state               |
| error state if needed                   |
| [Choose audio file]                     |
+----------------------------------------+
```

## Review Screen Layout

```text
+---------------- summary ----------------+
| source file                             |
| evidence summary                        |
| suggested draft name                    |
| draft set count / bpm                   |
+----------------------------------------+

+--------------- candidates --------------+
| top candidate scales                    |
+----------------------------------------+

+-------------- detected notes -----------+
| stable detected notes                   |
| timing/confidence text                  |
+----------------------------------------+

+--------------- actions -----------------+
| [Save scale]                            |
| later: [Edit draft] [Preview playback]  |
+----------------------------------------+
```

## UX Priorities

1. user should understand what file was analyzed
2. user should see the proposed result quickly
3. user should have enough evidence to trust or reject it
4. reinference should be an obvious correction path when only part of the draft is wrong
5. save should be easy when the result looks correct

## States

### Loading

- large simple progress state
- avoid noisy intermediate detail

### Success

- summary first
- evidence second
- reinfer/edit path visible before save
- save action fixed and obvious

### Failure

- one clear error message
- ability to choose another file

## Future UX Additions

- preview playback of the draft
- open in editor for correction using the selected-set piano roll
- infer missing sets from one or more corrected editor sets
- timing visualization between detected sounds
