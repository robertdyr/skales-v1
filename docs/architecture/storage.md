# Storage

## Responsibility

Storage persists approved final scales and retrieves them for the rest of the app.

## External Contract

```mermaid
flowchart LR
    ScaleA["Scale"] <--> Persistence["persistence"]
```

## Internal Shape

```mermaid
flowchart LR
    ScaleB["Scale"] <--> Repository["repository"] <--> Dao["DAO"] <--> Room["Room"]
```

## Current Code Mapping

```mermaid
flowchart TD
    StorageDir["storage/"] --> Repository2["ScaleRepository.kt"]
    StorageDir --> Local["local/"]
    Local --> Converters["Converters.kt"]
    Local --> ScaleDao["ScaleDao.kt"]
    Local --> ScaleEntity["ScaleEntity.kt"]
    Local --> Database["SkalesDatabase.kt"]
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
