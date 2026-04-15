# Analyzer

## Responsibility

The analyzer is the library that turns audio into a playable draft scale plus supporting evidence.

## External Contract

```text
Input  : audio file or recording
Output : ScaleDraft + evidence
```

More explicitly:

```text
audio -> analyzer -> {
  draftScale,
  detectedNotes,
  candidates,
  phraseInfo,
  optional timing inference,
  debug/review evidence
}
```

## Internal Shape

```text
+------------------------ analyzer ------------------------+
|                                                          |
|  input -> recognizer -> contextualizer -> draft-builder |
|            |                 |                  |        |
|            +------ evidence --+-----------------+        |
|                                                          |
+----------------------------------------------------------+
```

### `input`

Owns:

- file import
- later microphone capture
- normalization into `AudioRecording`

Current examples:

- `AudioFileImporter`
- future `AudioRecorder`

### `recognizer`

Owns:

- audio decoding
- pitch extraction
- pitch frame cleanup
- reduction into stable note events

Current examples:

- `AndroidAudioDecoder`
- `AudioFilePitchDetector`
- `DefaultPitchFrameFilter`
- `DefaultNoteEventReducer`

Recommended future abstraction:

```kotlin
interface NoteExtractor {
    suspend fun extract(recording: AudioRecording): NoteExtractionResult
}
```

### `contextualizer`

Owns:

- phrase segmentation
- candidate scale ranking
- interpretation of detected notes as musical structure
- later timing inference between sounds and sets
- optional LLM reasoning over structured evidence

Current examples:

- `DefaultPhraseSegmenter`
- `DefaultScaleCandidateRanker`

Recommended future abstractions:

```kotlin
interface ScaleContextualizer {
    fun contextualize(extraction: NoteExtractionResult): ContextualizedScaleEvidence
}

interface TimingInferencer {
    fun infer(extraction: NoteExtractionResult): InferredTiming
}
```

Important rule:

- if an LLM is used, it belongs after note extraction, not instead of note extraction

### `draft-builder`

Owns:

- conversion from interpreted evidence into `ScaleDraft`

Current example:

- `DefaultScaleDraftBuilder`

## Current Runtime Flow

```text
picked file
  -> AudioFileImporter
  -> AudioRecording
  -> RecordingAnalysisPipeline
  -> ScaleDraft + evidence
  -> review screen
```

## What The Analyzer Must Not Know

- Compose screen layout
- navigation routes
- final Room persistence details
- playback UI state

## Good Future Direction

Best cleanup from the current implementation:

```text
RecordingAnalysisPipeline
    becomes the top-level Analyzer facade
    over smaller explicit interfaces:
      NoteExtractor
      ScaleContextualizer
      TimingInferencer
      ScaleDraftBuilder
```

## Testing Strategy

```text
audio fixture -> recognizer     -> note assertions
note fixture  -> contextualizer -> phrase/candidate assertions
audio fixture -> analyzer       -> draft assertions
```
