package com.example.skales.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.player.PlaybackCursor
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSoundKind
import com.example.skales.editor.PianoKeyboard
import com.example.skales.editor.ScaleEditorViewModel
import com.example.skales.editor.labelForSound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleEditorScreen(
    viewModel: ScaleEditorViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit scale" else "New scale") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Scale name") },
                singleLine = true,
            )

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Playback", style = MaterialTheme.typography.titleMedium)
                    SettingStepper(
                        label = "Tempo",
                        value = "${uiState.bpm} BPM",
                        onDecrease = viewModel::decreaseBpm,
                        onIncrease = viewModel::increaseBpm,
                    )
                    Text(
                        text = playbackCursorLabel(uiState.playbackCursor),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = viewModel::stepScale,
                            enabled = uiState.sets.any { it.sounds.isNotEmpty() } && !uiState.isPlaying,
                        ) {
                            Text("Step")
                        }
                        TextButton(
                            onClick = viewModel::playScale,
                            enabled = uiState.sets.any { it.sounds.isNotEmpty() } && !uiState.isPlaying,
                        ) {
                            Text(if (uiState.isPlaying) "Playing..." else "Play")
                        }
                        TextButton(onClick = viewModel::stopScale, enabled = uiState.isPlaying) {
                            Text("Stop")
                        }
                        TextButton(onClick = viewModel::resetPlaybackCursor) {
                            Text("Reset")
                        }
                    }
                }
            }

            PianoKeyboard(
                onNotePressed = viewModel::onNotePressed,
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Sets", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = viewModel::addSet) {
                            Text("New set")
                        }
                        TextButton(onClick = viewModel::addChordCueToSelectedSet) {
                            Text("Add chord cue")
                        }
                        TextButton(
                            onClick = viewModel::removeChordCueFromSelectedSet,
                            enabled = uiState.selectedSet?.sounds?.firstOrNull()?.kind == ScaleSoundKind.Cue,
                        ) {
                            Text("Remove chord cue")
                        }
                        TextButton(onClick = viewModel::deleteSelectedSet, enabled = uiState.sets.isNotEmpty()) {
                            Text("Delete set")
                        }
                    }
                    if (uiState.sets.isEmpty()) {
                        Text(
                            text = "Create a set, then tap notes on the keyboard.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            uiState.sets.forEachIndexed { index, set ->
                                SetCard(
                                    setIndex = index,
                                    set = set,
                                    isSelected = index == uiState.selectedSetIndex,
                                    onSelect = { viewModel.selectSet(index) },
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = viewModel::removeLastSoundFromSelectedSet,
                            enabled = uiState.selectedSet?.sounds?.isNotEmpty() == true,
                        ) {
                            Text("Remove last sound")
                        }
                        TextButton(
                            onClick = viewModel::clearSelectedSet,
                            enabled = uiState.selectedSet?.sounds?.isNotEmpty() == true,
                        ) {
                            Text("Clear selected set")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.saveScale(onNavigateBack) },
                    enabled = uiState.name.isNotBlank() && uiState.sets.any { it.sounds.isNotEmpty() },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SetCard(
    setIndex: Int,
    set: ScaleSet,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        onClick = onSelect,
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        shape = MaterialTheme.shapes.large,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Set ${setIndex + 1}${set.breakAfterBeats?.let { "  (+${it} beat break)" } ?: ""}",
                style = MaterialTheme.typography.titleSmall,
            )
            if (set.sounds.isEmpty()) {
                Text(text = "Empty set", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    set.sounds.forEachIndexed { soundIndex, sound ->
                        val prefix = if (sound.kind == ScaleSoundKind.Cue) "C" else "N"
                        val breakLabel = sound.breakAfterBeats?.let { "  -$it" } ?: ""
                        AssistChip(
                            onClick = onSelect,
                            label = {
                                Text("${prefix}${soundIndex + 1}: ${labelForSound(sound)}$breakLabel")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDecrease) {
                Text("-")
            }
            TextButton(onClick = onIncrease) {
                Text("+")
            }
        }
    }
}

private fun playbackCursorLabel(cursor: PlaybackCursor): String {
    return if (cursor.isFinished) {
        "Cursor: finished"
    } else {
        "Cursor: set ${cursor.setIndex + 1}, sound ${cursor.soundIndexInSet + 1}"
    }
}
