# Storage

## Responsibility

Storage persists approved final scales and retrieves them for the rest of the app.

## External Contract

```text
Scale <-> persistence
```

## Internal Shape

```text
+---------------- storage ----------------+
|                                         |
|  Scale <-> repository <-> DAO <-> Room  |
|                                         |
+-----------------------------------------+
```

## Current Code Mapping

```text
storage/
├── ScaleRepository.kt
└── local/
    ├── Converters.kt
    ├── ScaleDao.kt
    ├── ScaleEntity.kt
    └── SkalesDatabase.kt
```

## Current Rule

Storage is for final approved playback objects.

Preferred rule:

- store `Scale`
- do not store arbitrary raw analyzer internals unless there is a clear product need

## What Storage Must Not Know

- Compose layout state
- pitch recognition logic
- playback scheduling details
