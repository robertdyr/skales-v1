package com.example.skales.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.skales.model.Note
import com.example.skales.model.PlaybackTiming
import com.example.skales.model.Scale
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
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
    val sets: List<ScaleSet> = listOf(ScaleSet(sounds = emptyList())),
    val selectedSetIndex: Int = 0,
    val bpm: Int = DefaultBpm,
    val playbackCursor: PlaybackCursor = PlaybackCursor(),
    val isEditing: Boolean = false,
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
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
                            sets = scale.sets.ifEmpty { listOf(ScaleSet(sounds = emptyList())) },
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

    fun selectSet(index: Int) {
        _uiState.update { state ->
            state.copy(selectedSetIndex = index.coerceIn(0, state.sets.lastIndex.coerceAtLeast(0)))
        }
    }

    fun addSet() {
        mutateSets { sets, _ ->
            val nextSets = sets + ScaleSet(sounds = emptyList())
            nextSets to nextSets.lastIndex
        }
    }

    fun deleteSelectedSet() {
        val state = uiState.value
        if (state.sets.isEmpty()) return

        mutateSets { sets, selectedIndex ->
            if (sets.size == 1) {
                listOf(ScaleSet(sounds = emptyList())) to 0
            } else {
                val nextSets = sets.toMutableList().apply { removeAt(selectedIndex) }
                nextSets to selectedIndex.coerceAtMost(nextSets.lastIndex)
            }
        }
    }

    fun addChordCueToSelectedSet() {
        mutateSelectedSet { set ->
            val chordNotes = set.sounds
                .filter { it.kind == ScaleSoundKind.Note }
                .flatMap { it.notes }
                .distinct()
                .take(3)
            if (chordNotes.isEmpty()) {
                set
            } else {
                val cue = ScaleSound(notes = chordNotes, kind = ScaleSoundKind.Cue)
                val existingFirstCue = set.sounds.firstOrNull()?.kind == ScaleSoundKind.Cue
                val updatedSounds = if (existingFirstCue) {
                    listOf(cue) + set.sounds.drop(1)
                } else {
                    listOf(cue) + set.sounds
                }
                set.copy(sounds = updatedSounds)
            }
        }
    }

    fun removeChordCueFromSelectedSet() {
        mutateSelectedSet { set ->
            set.copy(sounds = set.sounds.filterIndexed { index, sound ->
                !(index == 0 && sound.kind == ScaleSoundKind.Cue)
            })
        }
    }

    fun onNotePressed(midi: Int) {
        viewModelScope.launch {
            pianoSoundPlayer.playSound(listOf(midi))
        }

        mutateSelectedSet { set ->
            set.copy(
                sounds = set.sounds + ScaleSound(
                    notes = listOf(midi),
                    kind = ScaleSoundKind.Note,
                ),
            )
        }
    }

    fun removeLastSoundFromSelectedSet() {
        mutateSelectedSet { set ->
            if (set.sounds.isEmpty()) {
                set
            } else {
                set.copy(sounds = set.sounds.dropLast(1))
            }
        }
    }

    fun clearSelectedSet() {
        mutateSelectedSet { set -> set.copy(sounds = emptyList()) }
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
        val scale = buildSavableScale() ?: return

        viewModelScope.launch {
            val id = scaleId ?: UUID.randomUUID().toString()
            scaleRepository.upsert(
                scale.copy(id = id),
                createdAt = persistedCreatedAt,
            )
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

    private fun currentPlayableScale(): Scale? {
        return buildDraftScale()?.takeIf { scale -> scale.sets.any { it.sounds.isNotEmpty() } }
    }

    private fun buildSavableScale(): Scale? {
        val draft = buildDraftScale() ?: return null
        if (draft.name.isBlank()) return null
        return draft.copy(name = draft.name.trim())
    }

    private fun buildDraftScale(): Scale? {
        val state = uiState.value

        val sets = state.sets
            .map { set -> set.copy(sounds = set.sounds.filter { sound -> sound.notes.isNotEmpty() }) }
            .filter { it.sounds.isNotEmpty() }
        if (sets.isEmpty()) return null

        return Scale(
            id = state.scaleId.orEmpty(),
            name = state.name,
            sets = sets,
            timing = PlaybackTiming(defaultBpm = state.bpm),
        )
    }

    private fun mutateSelectedSet(transform: (ScaleSet) -> ScaleSet) {
        mutateSets { sets, selectedIndex ->
            val ensuredSets = ensureSetList(sets, selectedIndex)
            val index = selectedIndex.coerceIn(0, ensuredSets.lastIndex)
            val nextSets = ensuredSets.toMutableList().apply {
                this[index] = transform(this[index])
            }
            nextSets to index
        }
    }

    private fun mutateSets(transform: (List<ScaleSet>, Int) -> Pair<List<ScaleSet>, Int>) {
        stopScale()
        _uiState.update { state ->
            val (nextSets, nextSelectedIndex) = transform(state.sets, state.selectedSetIndex)
            state.copy(
                sets = ensureSetList(nextSets, nextSelectedIndex),
                selectedSetIndex = nextSelectedIndex.coerceIn(0, ensureSetList(nextSets, nextSelectedIndex).lastIndex),
                playbackCursor = PlaybackCursor(),
            )
        }
    }

    private fun ensureSetList(sets: List<ScaleSet>, selectedIndex: Int): List<ScaleSet> {
        return if (sets.isEmpty()) {
            listOf(ScaleSet(sounds = emptyList()))
        } else if (selectedIndex > sets.lastIndex) {
            sets
        } else {
            sets
        }
    }
}

fun labelForSound(sound: ScaleSound): String {
    return sound.notes.joinToString(separator = " + ") { midi ->
        val note = Note.fromMidi(midi)
        "${note.name}${note.octave}"
    }
}
