package com.example.skales.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.skales.editor.ScaleEditorOps
import com.example.skales.editor.SetGridOps
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSoundKind
import com.example.skales.player.PianoSoundPlayer
import com.example.skales.player.PlaybackCursor
import com.example.skales.player.PlaybackDirection
import com.example.skales.player.ScaleAutoPlayer
import com.example.skales.player.ScaleStepper
import com.example.skales.player.normalizedFor
import com.example.skales.storage.ScaleRepository
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DefaultBpm = 92
private const val MinBpm = 40
private const val MaxBpm = 240
private const val BpmStep = 4

data class ScaleEditorUiState(
    val scaleId: String? = null,
    val name: String = "",
    val sets: List<ScaleSet> = ScaleEditorOps.defaultSets(),
    val selectedSetIndex: Int = 0,
    val placementKind: ScaleSoundKind = ScaleSoundKind.Note,
    val snapStepBeats: Float = SetGridOps.DefaultStepBeats,
    val bpm: Int = DefaultBpm,
    val playbackCursor: PlaybackCursor = PlaybackCursor(),
    val isEditing: Boolean = false,
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val isKeyboardExpanded: Boolean = false,
) {
    val selectedSet: ScaleSet?
        get() = sets.getOrNull(selectedSetIndex)
}

class ScaleEditorViewModel(
    private val scaleRepository: ScaleRepository,
    private val pianoSoundPlayer: PianoSoundPlayer,
    private val scaleStepper: ScaleStepper,
    private val scaleAutoPlayer: ScaleAutoPlayer,
    private val scaleId: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScaleEditorUiState())
    val uiState: StateFlow<ScaleEditorUiState> = _uiState.asStateFlow()
    private var playbackJob: Job? = null
    private var persistedCreatedAt: Long? = null

    init {
        if (scaleId == null) {
            _uiState.update { it.copy(isLoaded = true) }
        } else {
            viewModelScope.launch {
                val scale = scaleRepository.getScale(scaleId)
                _uiState.update {
                    if (scale == null) {
                        it.copy(isLoaded = true)
                    } else {
                        it.copy(
                            scaleId = scale.id,
                            name = scale.name,
                            sets = ScaleEditorOps.normalizeSets(scale.sets),
                            selectedSetIndex = 0,
                            bpm = scale.timing.defaultBpm,
                            playbackCursor = PlaybackCursor(),
                            isEditing = true,
                            isLoaded = true,
                        )
                    }
                }
                persistedCreatedAt = scaleRepository.getScaleCreatedAt(scaleId)
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun increaseBpm() {
        _uiState.update { it.copy(bpm = (it.bpm + BpmStep).coerceAtMost(MaxBpm)) }
    }

    fun decreaseBpm() {
        _uiState.update { it.copy(bpm = (it.bpm - BpmStep).coerceAtLeast(MinBpm)) }
    }

    fun setKeyboardExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isKeyboardExpanded = expanded) }
    }

    fun selectSet(index: Int) {
        stopScale()
        _uiState.update { state ->
            state.copy(
                selectedSetIndex = ScaleEditorOps.normalizeSelectedSetIndex(state.sets, index),
                playbackCursor = PlaybackCursor(),
            )
        }
    }

    fun addSet() {
        mutateSets { sets, _ -> ScaleEditorOps.addSet(sets) }
    }

    fun deleteSelectedSet() {
        mutateSets(ScaleEditorOps::deleteSelectedSet)
    }

    fun setSnapStepBeats(stepBeats: Float) {
        _uiState.update { it.copy(snapStepBeats = stepBeats) }
    }

    fun setPlacementKind(kind: ScaleSoundKind) {
        _uiState.update { it.copy(placementKind = kind) }
    }

    fun onNotePressed(midi: Int) {
        previewMidi(midi)
        stopScale()
        _uiState.update { state ->
            val nextSets = ScaleEditorOps.addSoundToSelectedSetAtColumn(
                sets = state.sets,
                selectedSetIndex = state.selectedSetIndex,
                midi = midi,
                column = SetGridOps.nextFreeColumn(
                    state.selectedSet ?: ScaleSet(sounds = emptyList()),
                    state.snapStepBeats,
                ),
                kind = state.placementKind,
                stepBeats = state.snapStepBeats,
            )
            val normalizedSets = ScaleEditorOps.normalizeSets(nextSets)
            state.copy(
                sets = normalizedSets,
                playbackCursor = PlaybackCursor(),
            )
        }
    }

    fun addSoundToSelectedSetAtPosition(column: Int, midi: Int) {
        previewMidi(midi)
        stopScale()
        _uiState.update { state ->
            val nextSets = ScaleEditorOps.addSoundToSelectedSetAtColumn(
                sets = state.sets,
                selectedSetIndex = state.selectedSetIndex,
                midi = midi,
                column = column,
                kind = state.placementKind,
                stepBeats = state.snapStepBeats,
            )
            val normalizedSets = ScaleEditorOps.normalizeSets(nextSets)
            state.copy(
                sets = normalizedSets,
                playbackCursor = PlaybackCursor(),
            )
        }
    }

    fun moveNoteInSelectedSet(soundId: String, midi: Int, column: Int) {
        previewMidi(midi)
        mutateSelectedSet { sets, selectedSetIndex ->
            ScaleEditorOps.moveNoteInSelectedSet(sets, selectedSetIndex, soundId, midi, column, uiState.value.snapStepBeats)
        }
    }

    fun moveSetBoundary(targetSetIndex: Int, column: Int) {
        stopScale()
        _uiState.update { state ->
            val nextSets = ScaleEditorOps.moveSetBoundary(
                sets = state.sets,
                targetSetIndex = targetSetIndex,
                column = column,
                stepBeats = state.snapStepBeats,
            )
            state.copy(
                sets = ScaleEditorOps.normalizeSets(nextSets),
                playbackCursor = PlaybackCursor(),
            )
        }
    }

    fun removeNoteFromSelectedSet(soundId: String) {
        mutateSelectedSet { sets, selectedSetIndex ->
            ScaleEditorOps.removeNoteFromSelectedSet(sets, selectedSetIndex, soundId, uiState.value.snapStepBeats)
        }
    }

    fun removeLastSoundFromSelectedSet() {
        mutateSelectedSet(ScaleEditorOps::removeLastSoundFromSelectedSet)
    }

    fun clearSelectedSet() {
        mutateSelectedSet(ScaleEditorOps::clearSelectedSet)
    }

    fun resetPlaybackCursor() {
        stopScale()
        _uiState.update { it.copy(playbackCursor = PlaybackCursor()) }
    }

    fun stepScale() {
        if (uiState.value.isPlaying) return
        val scale = currentPlayableScale() ?: return

        viewModelScope.launch {
            val currentCursor = uiState.value.playbackCursor.normalizedFor(scale, PlaybackDirection.Forward)
            val step = scaleStepper.next(scale, currentCursor, PlaybackDirection.Forward)
            _uiState.update { it.copy(playbackCursor = step.nextCursor) }
        }
    }

    fun playScale() {
        val scale = currentPlayableScale() ?: return
        if (uiState.value.isPlaying) return

        playbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            try {
                scaleAutoPlayer.play(
                    scale = scale,
                    initialCursor = uiState.value.playbackCursor,
                    bpmProvider = { uiState.value.bpm },
                    directionProvider = { PlaybackDirection.Forward },
                    updateCursor = { cursor ->
                        _uiState.update { it.copy(playbackCursor = cursor) }
                    },
                )
            } finally {
                _uiState.update { it.copy(isPlaying = false) }
            }
        }
    }

    fun stopScale() {
        playbackJob?.cancel()
        playbackJob = null
        pianoSoundPlayer.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun saveScale(onSaved: () -> Unit) {
        val state = uiState.value
        val scale = ScaleEditorOps.buildSavableScale(state.scaleId, state.name, state.sets, state.bpm) ?: return

        viewModelScope.launch {
            val id = scaleId ?: UUID.randomUUID().toString()
            scaleRepository.upsert(scale.copy(id = id), createdAt = persistedCreatedAt)
            onSaved()
        }
    }

    override fun onCleared() {
        playbackJob?.cancel()
        pianoSoundPlayer.stop()
    }

    companion object {
        fun factory(
            scaleRepository: ScaleRepository,
            pianoSoundPlayer: PianoSoundPlayer,
            scaleStepper: ScaleStepper,
            scaleAutoPlayer: ScaleAutoPlayer,
            scaleId: String?,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ScaleEditorViewModel(
                        scaleRepository = scaleRepository,
                        pianoSoundPlayer = pianoSoundPlayer,
                        scaleStepper = scaleStepper,
                        scaleAutoPlayer = scaleAutoPlayer,
                        scaleId = scaleId,
                    ) as T
                }
            }
        }
    }

    private fun currentPlayableScale() = uiState.value.let { state ->
        ScaleEditorOps.buildDraftScale(state.scaleId, state.name, state.sets, state.bpm)
    }

    private fun previewMidi(midi: Int) {
        viewModelScope.launch {
            pianoSoundPlayer.playSound(listOf(midi))
        }
    }

    private fun mutateSelectedSet(transform: (List<ScaleSet>, Int) -> List<ScaleSet>) {
        stopScale()
        _uiState.update { state ->
            val nextSets = transform(state.sets, state.selectedSetIndex)
            val normalizedSets = ScaleEditorOps.normalizeSets(nextSets)
            state.copy(
                sets = normalizedSets,
                selectedSetIndex = ScaleEditorOps.normalizeSelectedSetIndex(normalizedSets, state.selectedSetIndex),
                playbackCursor = PlaybackCursor(),
            )
        }
    }

    private fun mutateSets(transform: (List<ScaleSet>, Int) -> Pair<List<ScaleSet>, Int>) {
        stopScale()
        _uiState.update { state ->
            val (nextSets, nextSelectedIndex) = transform(state.sets, state.selectedSetIndex)
            val normalizedSets = ScaleEditorOps.normalizeSets(nextSets)
            state.copy(
                sets = normalizedSets,
                selectedSetIndex = ScaleEditorOps.normalizeSelectedSetIndex(normalizedSets, nextSelectedIndex),
                playbackCursor = PlaybackCursor(),
            )
        }
    }
}
