# Skales — Agent Context

This file contains the full context for the Skales project. Read this before doing anything.

---

## Repo Layout

```
skales/
  AGENT.md               -- this file
  skales-android/        -- Android app (Jetpack Compose)
  skales-backend/        -- Backend proxy service (user's own stack)
```

The Android app and backend are separate projects in the same monorepo.
When working on the app, `cd skales-android/`. When working on the backend, `cd skales-backend/`.

---

## What is Skales

A singing practice app. The user is a singing student. Existing scale practice apps have pre-built
scales only. Skales lets you define your own scales and uses AI to suggest scales from a short
recording.

The user is a **distributed systems / backend / platform engineer**, not an Android dev.
Skip beginner explanations. Use correct terminology. Treat them like a senior engineer learning
a new platform.

---

## Core Product Vision

### The main feature: custom scale builder
- User builds scales by tapping notes on an on-screen piano keyboard
- Notes are saved as an ordered sequence
- User can name, save, edit, delete scales
- User can play back a scale (piano sound)
- User can practice by singing along to playback

### The secondary feature: AI suggestion engine
- User records a short audio clip of themselves singing a few notes
- App does pitch detection on the recording (post-recording batch, not real-time)
- Detected note sequence is sent to a **user-owned backend proxy**
- Backend calls an LLM with the note sequence
- LLM suggests what scale pattern this could be / how to complete it
- App shows suggestions; user can accept (auto-fill scale builder) or ignore
- This is a **suggestion engine**, not a ground truth detector — user corrects it by adding more notes

### Key design decisions already made
- AI layer is additive, not load-bearing — app works fully without it
- LLM gets symbolic input (note names + intervals + semitone patterns), not raw audio
- Store rich note data (both note names AND intervals AND semitone offsets from root)
  so we can experiment with what representation gives best LLM accuracy
- No KMP for now — Android only first
- Backend is the user's own service — app just calls an HTTP endpoint
- Pitch detection: post-recording batch processing (not real-time)
- Playback: piano-quality sampled sounds (not sine wave synthesis)
- Piano keyboard: 2 octaves (C3 to C5), replaces a real piano for people who don't own one

---

## Architecture

### Stack
- **Language**: Kotlin (AGP 9.x bundles Kotlin — no standalone kotlin-android plugin needed)
- **UI**: Jetpack Compose + Material3
- **Architecture pattern**: MVVM
- **Async**: Coroutines + StateFlow/Flow
- **Local DB**: Room (use annotationProcessor, not KSP, with AGP 9.x)
- **Navigation**: Navigation Compose
- **Audio recording**: Android AudioRecord API
- **Pitch detection**: TarsosDSP library
- **Piano playback**: SoundPool with sampled piano notes (or MIDI synthesis)
- **Networking**: Ktor client (or OkHttp) for backend calls

### Package structure
```
com.example.skales/
  MainActivity.kt
  ui/
    theme/         -- Color.kt, Theme.kt
    screens/       -- ScaleListScreen.kt, ScaleEditorScreen.kt, RecordScreen.kt
    components/    -- PianoKeyboard.kt, NoteChip.kt, etc.
    navigation/    -- NavGraph.kt
  viewmodel/       -- ScaleListViewModel.kt, ScaleEditorViewModel.kt, RecordViewModel.kt
  data/
    local/         -- ScaleEntity.kt, ScaleDao.kt, SkalesDatabase.kt
    repository/    -- ScaleRepository.kt
  domain/
    model/         -- Note.kt, Scale.kt
  audio/           -- AudioRecorder.kt, PitchDetector.kt
```

### MVVM flow
```
Screen (Compose) 
  --> observes StateFlow from ViewModel
  --> calls methods on ViewModel
ViewModel 
  --> holds UI state as StateFlow<SomeState>
  --> calls Repository / UseCases
  --> does NOT touch Android View system
Repository 
  --> wraps Room DAO (local)
  --> wraps API client (remote)
  --> exposes Flow<T>
```

---

## Domain Model

### Note
```kotlin
data class Note(
    val name: String,       // "C", "C#", "D", etc.
    val midiNumber: Int,    // 0-127, middle C = 60
    val octave: Int,        // middle C is octave 4
    val isBlackKey: Boolean,
)
// Note.fromMidi(60) -> Note("C", 60, 4, false)
// note.frequency -> Hz (A4=440 standard tuning)
// note.displayName -> "C4"
```

### Scale
```kotlin
data class Scale(
    val id: String,              // UUID
    val name: String,            // user-given name
    val notes: List<Int>,        // ordered MIDI numbers
    val createdAt: Long,
    val updatedAt: Long,
    val source: ScaleSource,     // MANUAL | RECORDED | AI_SUGGESTED
)
// scale.noteNames       -> ["C4", "D4", "E4", ...]
// scale.intervals       -> semitone steps between consecutive notes [2, 2, 1, ...]
// scale.semitonePattern -> offsets from root [0, 2, 4, 5, ...]
```

### Why store all three representations
We don't know yet which representation gives the best LLM accuracy for scale pattern recognition.
- **Note names** ("C4 D4 E4") — human readable, carries absolute pitch and octave info
- **Intervals** ([2, 2, 1, 2, 2, 2, 1]) — the classic music theory representation
- **Semitone pattern** ([0, 2, 4, 5, 7, 9, 11]) — root-relative, transposable
We send all three in the LLM prompt so we can iterate on what works.

---

## Build Setup Notes (IMPORTANT)

### Android Studio version
Android Studio **2025.3.3 (Meerkat Feature Drop)** — ships with **AGP 9.1.0**.

### AGP 9.1.0 quirks
- **Bundles Kotlin internally** — do NOT add `kotlin-android` plugin, it will conflict
- **Bundles KSP internally** — do NOT add standalone `ksp` plugin, it will conflict
- **Still requires** `org.jetbrains.kotlin.plugin.compose` for Compose support
- **Room annotation processor**: use `annotationProcessor(...)` not `ksp(...)`
- **No `kotlinOptions {}` block** — AGP 9 manages Kotlin settings differently
- **`compileSdk`** uses new DSL: `compileSdk { version = release(36) { minorApiLevel = 1 } }`
  OR just `compileSdk = 36` (integer form also works)
- The recommended path is: **use Android Studio "Empty Activity" template** which generates
  a correctly configured project for AGP 9.x. Do not hand-write the gradle setup from scratch.

### How to bootstrap correctly
1. Create the `skales/` root folder
2. Inside it, create `skales-android/` and `skales-backend/`
3. Move this AGENT.md into `skales/`
4. In Android Studio: File > New > New Project > Empty Activity
5. Set save location to `skales/skales-android/`
6. Set: Language=Kotlin, Min SDK=26, Package=com.example.skales
7. Let Android Studio generate the Gradle files — they will be correct for AGP 9.1
8. Then start adding dependencies on top of what's generated
9. Open this AGENT.md and resume from Phase 2

### Versions known to be safe to add on top of the generated template
```toml
# Add these to the generated libs.versions.toml
navigationCompose = "2.8.5"
lifecycleRuntime = "2.8.7"
roomVersion = "2.6.1"
coroutines = "1.9.0"

# Dependencies to add to app/build.gradle.kts
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
annotationProcessor(libs.androidx.room.compiler)   # <-- annotationProcessor NOT ksp
implementation(libs.kotlinx.coroutines.android)
```

---

## Build Phases

### Phase 1: Bootstrap (DO THIS MANUALLY IN ANDROID STUDIO)
- Create `skales/skales-android/` via the "Empty Activity" template (see bootstrap steps above)
- Create `skales/skales-backend/` as an empty folder for now
- Verify the Android project builds and runs on device/emulator
- Add the extra dependencies listed above
- Copy the source files from the old project if they exist

### Phase 2: Domain model + Room DB (start here after bootstrap)
- `Note.kt` — note representation with fromMidi(), frequency, displayName
- `Scale.kt` — scale with noteNames, intervals, semitonePattern computed properties
- `ScaleEntity.kt` + `ScaleDao.kt` + `SkalesDatabase.kt`
- `ScaleRepository.kt` — wraps DAO, exposes Flow<List<Scale>>

### Phase 3: Scale list screen
- `ScaleListViewModel.kt` — StateFlow<List<Scale>>, delete()
- `ScaleListScreen.kt` — LazyColumn of scales, FAB to create new, swipe/button to delete
- Navigation skeleton

### Phase 4: Piano keyboard + Scale editor
- `PianoKeyboard.kt` — tappable 2-octave keyboard (C3-C5), highlights active notes
- `ScaleEditorViewModel.kt` — manages note sequence being built, save/load/reset
- `ScaleEditorScreen.kt` — name field + note chips + keyboard + interval display

### Phase 5: Scale playback
- `ScalePlayer.kt` — plays notes sequentially using SoundPool + piano samples
- Play button in ScaleEditorScreen and ScaleListScreen
- Configurable tempo (BPM)

### Phase 6: Audio recording + pitch detection
- `AudioRecorder.kt` — wraps AudioRecord API, saves PCM to temp file
- `PitchDetector.kt` — runs TarsosDSP YIN algorithm on recorded audio
- `RecordViewModel.kt` + `RecordScreen.kt`
- Request RECORD_AUDIO permission at runtime

### Phase 7: Backend + LLM suggestion
- `skales-backend/` — user implements this in their own stack
- Contract: POST `/suggest` with body `{ noteNames, intervals, semitonePattern, context }`
- Response: `{ suggestions: [{ name, notes, confidence, explanation }] }`
- `skales-android`: `SuggestionApiClient.kt` — HTTP client pointing at the backend URL
- `SuggestionViewModel.kt` + suggestion UI in RecordScreen
- Settings screen for backend URL configuration (so user can point at local dev or prod)

### Phase 8: Polish
- Practice session history
- Settings (default octave, BPM, backend URL)
- Error states, loading states, empty states
- Better theming

---

## Source files already written (may need minor fixes after bootstrap)

Located in `skales-android/app/src/main/java/com/example/skales/`:

- `domain/model/Note.kt` — complete
- `domain/model/Scale.kt` — complete
- `data/local/ScaleEntity.kt` — complete (Room entity + mappers)
- `data/local/ScaleDao.kt` — complete
- `data/local/SkalesDatabase.kt` — complete (singleton)
- `data/repository/ScaleRepository.kt` — complete
- `viewmodel/ScaleListViewModel.kt` — complete
- `viewmodel/ScaleEditorViewModel.kt` — complete
- `ui/theme/Color.kt` — complete
- `ui/theme/Theme.kt` — complete
- `ui/components/PianoKeyboard.kt` — complete (white + black keys, highlights)
- `ui/screens/ScaleListScreen.kt` — complete
- `ui/screens/ScaleEditorScreen.kt` — complete
- `ui/navigation/NavGraph.kt` — complete
- `MainActivity.kt` — complete

These files are all logically correct. The only reason the project doesn't build is the
AGP 9.1.0 Gradle setup, not the Kotlin source code.

---

## LLM Prompt Strategy (for Phase 7)

The LLM call should receive something like:

```
The user sang the following notes: C4, D4, E4, G4, A4

Represented as:
- Note names: C4 D4 E4 G4 A4
- Intervals from consecutive notes: 2, 2, 3, 2 semitones
- Semitone offsets from root: 0, 2, 4, 7, 9

Suggest what scale(s) this could be a fragment of. For each suggestion provide:
1. Scale name
2. Complete note set for one octave
3. Missing notes to complete the scale
4. Confidence level
5. Brief explanation

Format the response as JSON.
```

Experiment with sending all three representations vs just one to see which gives better results.

---

## Key Android concepts (for reference)

| Concept | What it is |
|---|---|
| `Activity` | Entry point of the app. In Compose apps, there's usually just one (MainActivity). |
| `Composable` | A function annotated `@Composable` that describes a piece of UI. Equivalent to a React component. |
| `ViewModel` | Survives screen rotations. Holds UI state. Lives longer than the Composable. |
| `StateFlow` | A hot Flow with a current value. Composables collect it with `collectAsStateWithLifecycle()`. |
| `Flow` | Cold async stream. Room DAOs return `Flow<T>` for live queries. |
| `LaunchedEffect` | Runs a suspend block when a Composable enters composition. Used for one-time triggers. |
| `remember` | Survives recomposition. Lost on screen rotation unless using `rememberSaveable`. |
| `Scaffold` | Layout with slots: topBar, bottomBar, FAB, content. |
| `NavController` | Manages the back stack. Navigate with `navController.navigate("route")`. |
| `Modifier` | Chain of layout/drawing instructions. Similar to CSS but functional. |
