package com.example.skales.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.skales.player.PianoSoundPlayer
import com.example.skales.player.PlaybackCursor
import com.example.skales.player.PlaybackDirection
import com.example.skales.player.ScaleAutoPlayer
import com.example.skales.player.ScaleStepper
import com.example.skales.player.normalizedFor
import com.example.skales.storage.ScaleRepository
import com.example.skales.model.Note
import com.example.skales.model.Scale
import com.example.skales.model.ScaleSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MinBpm = 40
private const val MaxBpm = 240
private const val BpmStep = 4

data class ScalePlayerUiState(
    val scale: Scale? = null,
    val bpm: Int = 92,
    val direction: PlaybackDirection = PlaybackDirection.Forward,
    val playbackCursor: PlaybackCursor = PlaybackCursor(),
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
) {
    val currentSet: ScaleSet?
        get() = scale?.sets?.getOrNull(playbackCursor.currentSetIndex(scale, direction))
}

class ScalePlayerViewModel(
    private val scaleRepository: ScaleRepository,
    private val pianoSoundPlayer: PianoSoundPlayer,
    private val scaleStepper: ScaleStepper,
    private val scaleAutoPlayer: ScaleAutoPlayer,
    private val scaleId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScalePlayerUiState())
    val uiState: StateFlow<ScalePlayerUiState> = _uiState.asStateFlow()
    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            val scale = scaleRepository.getScale(scaleId)
            _uiState.update {
                it.copy(
                    scale = scale,
                    bpm = scale?.timing?.defaultBpm ?: it.bpm,
                    isLoaded = true,
                )
            }
        }
    }

    fun increaseBpm() {
        _uiState.update { it.copy(bpm = (it.bpm + BpmStep).coerceAtMost(MaxBpm)) }
    }

    fun decreaseBpm() {
        _uiState.update { it.copy(bpm = (it.bpm - BpmStep).coerceAtLeast(MinBpm)) }
    }

    fun setDirection(direction: PlaybackDirection) {
        stopPlayback()
        _uiState.update { state ->
            val scale = state.scale
            val nextCursor = if (scale == null) {
                PlaybackCursor()
            } else {
                state.playbackCursor.normalizedFor(scale, direction)
            }
            state.copy(direction = direction, playbackCursor = nextCursor)
        }
    }

    fun step() {
        val state = uiState.value
        val scale = state.scale ?: return
        if (state.isPlaying) return

        viewModelScope.launch {
            val currentCursor = uiState.value.playbackCursor.normalizedFor(scale, uiState.value.direction)
            val step = scaleStepper.next(scale, currentCursor, uiState.value.direction)
            _uiState.update { it.copy(playbackCursor = step.nextCursor) }
        }
    }

    fun play() {
        val state = uiState.value
        val scale = state.scale ?: return
        if (state.isPlaying) return

        playbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            try {
                scaleAutoPlayer.play(
                    scale = scale,
                    initialCursor = uiState.value.playbackCursor,
                    bpmProvider = { uiState.value.bpm },
                    directionProvider = { uiState.value.direction },
                    updateCursor = { cursor ->
                        _uiState.update { it.copy(playbackCursor = cursor) }
                    },
                )
            } finally {
                _uiState.update { it.copy(isPlaying = false) }
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        pianoSoundPlayer.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun resetCursor() {
        stopPlayback()
        _uiState.update { it.copy(playbackCursor = PlaybackCursor()) }
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
            scaleId: String,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ScalePlayerViewModel(
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
}

fun PlaybackCursor.currentSetIndex(scale: Scale?, direction: PlaybackDirection): Int {
    if (scale == null) return 0
    val playableScale = scale.withoutEmptySetsForPlayer()
    if (playableScale.sets.isEmpty()) return 0
    val normalized = normalizedFor(playableScale, direction)
    return normalized.setIndex.coerceIn(0, playableScale.sets.lastIndex)
}

fun labelForSetSound(set: ScaleSet, soundIndex: Int): String {
    val sound = set.sounds[soundIndex]
    return sound.notes.joinToString(separator = " ") { midi ->
        Note.fromMidi(midi).name
    }
}

private fun Scale.withoutEmptySetsForPlayer(): Scale = copy(
    sets = sets
        .map { set -> set.copy(sounds = set.sounds.filter { it.notes.isNotEmpty() }) }
        .filter { it.sounds.isNotEmpty() },
)
