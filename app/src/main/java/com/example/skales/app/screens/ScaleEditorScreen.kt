package com.example.skales.app.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.app.components.PianoKeyboard
import com.example.skales.app.components.SetPianoRollEditor
import com.example.skales.app.components.SkalesBackground
import com.example.skales.app.components.SkalesCircleButton
import com.example.skales.app.components.SkalesMetric
import com.example.skales.app.components.SkalesPanel
import com.example.skales.app.components.SkalesPill
import com.example.skales.app.components.SkalesPrimaryButton
import com.example.skales.app.components.SkalesSecondaryButton
import com.example.skales.app.components.SkalesSectionHeader
import com.example.skales.app.components.SkalesWordmark
import com.example.skales.app.viewmodel.ScaleEditorViewModel
import com.example.skales.editor.ScaleEditorOps
import com.example.skales.editor.SetGridOps
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSoundKind
import com.example.skales.player.PlaybackCursor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleEditorScreen(
    viewModel: ScaleEditorViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(if (uiState.isEditing) "Edit scale" else "New scale") },
                navigationIcon = {
                    SkalesSecondaryButton(text = "Back", onClick = onNavigateBack)
                },
            )
        },
    ) { innerPadding ->
        if (!uiState.isLoaded) {
            SkalesBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SkalesPanel { Text("Loading...") }
                }
            }
            return@Scaffold
        }

        SkalesBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SkalesPanel {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SkalesWordmark(compact = true)
                        SkalesPill(text = if (uiState.isEditing) "Editing" else "New scale", highlighted = true)
                    }
                }

                SkalesPanel {
                    SkalesSectionHeader(
                        title = "Identity",
                        supporting = "Give the scale a clear practice label before building its sets.",
                    )
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Scale name") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        ),
                    )
                }

                SkalesPanel {
                    SkalesSectionHeader(
                        title = "Playback preview",
                        supporting = playbackCursorLabel(uiState.playbackCursor),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkalesMetric(label = "Tempo", value = "${uiState.bpm} BPM")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SkalesCircleButton(label = "-", onClick = viewModel::decreaseBpm, size = 48.dp)
                            SkalesCircleButton(label = "+", onClick = viewModel::increaseBpm, size = 48.dp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SkalesPrimaryButton(
                            text = if (uiState.isPlaying) "Playing" else "Play",
                            onClick = viewModel::playScale,
                            enabled = uiState.sets.any { it.sounds.isNotEmpty() } && !uiState.isPlaying,
                        )
                        SkalesSecondaryButton(
                            text = "Step",
                            onClick = viewModel::stepScale,
                            enabled = uiState.sets.any { it.sounds.isNotEmpty() } && !uiState.isPlaying,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkalesSecondaryButton(text = "Reset", onClick = viewModel::resetPlaybackCursor)
                        SkalesSecondaryButton(
                            text = "Stop",
                            onClick = viewModel::stopScale,
                            enabled = uiState.isPlaying,
                        )
                    }
                }

                SkalesPanel {
                    SkalesSectionHeader(
                        title = "Set strip",
                        supporting = "Only the selected set opens in the piano roll, so dragging notes never conflicts with other sets.",
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.sets.forEachIndexed { index, set ->
                            SetSelectorPill(
                                setIndex = index,
                                set = set,
                                isSelected = index == uiState.selectedSetIndex,
                                onSelect = { viewModel.selectSet(index) },
                            )
                        }
                    }
                }

                SkalesPanel {
                    SkalesSectionHeader(
                        title = "Piano roll",
                        supporting = "Tap a key to arm a pitch, tap empty grid space to place it, and drag blocks to change pitch or spacing.",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SnapChip(
                            text = "1/1",
                            selected = uiState.snapStepBeats == SetGridOps.CoarseStepBeats,
                            onClick = { viewModel.setSnapStepBeats(SetGridOps.CoarseStepBeats) },
                        )
                        SnapChip(
                            text = "1/2 beat",
                            selected = uiState.snapStepBeats == SetGridOps.DefaultStepBeats,
                            onClick = { viewModel.setSnapStepBeats(SetGridOps.DefaultStepBeats) },
                        )
                        SnapChip(
                            text = "1/4 beat",
                            selected = uiState.snapStepBeats == SetGridOps.FineStepBeats,
                            onClick = { viewModel.setSnapStepBeats(SetGridOps.FineStepBeats) },
                        )
                    }
                    val selectedSet = uiState.selectedSet ?: ScaleSet(sounds = emptyList())
                    SetPianoRollEditor(
                        grid = SetGridOps.toGrid(selectedSet, stepBeats = uiState.snapStepBeats),
                        armedMidi = uiState.armedMidi,
                        onCellTap = viewModel::addArmedOrDirectNoteToSelectedSet,
                        onNoteMove = viewModel::moveNoteInSelectedSet,
                        onDeleteNote = viewModel::removeNoteFromSelectedSet,
                    )
                }

                SkalesPanel {
                    SkalesSectionHeader(
                        title = "Keyboard",
                        supporting = "Keyboard taps preview and arm a pitch for placement in the piano roll.",
                    )
                    PianoKeyboard(
                        onNotePressed = viewModel::onNotePressed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SkalesPanel {
                    SkalesSectionHeader(
                        title = "Sets",
                        supporting = "Keep one set selected while adding notes or cue sounds.",
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EditorActionChip(text = "New set", onClick = viewModel::addSet)
                        EditorActionChip(text = "Add chord cue", onClick = viewModel::addChordCueToSelectedSet)
                        EditorActionChip(
                            text = "Remove cue",
                            onClick = viewModel::removeChordCueFromSelectedSet,
                            enabled = uiState.selectedSet?.sounds?.firstOrNull()?.kind == ScaleSoundKind.Cue,
                        )
                        EditorActionChip(
                            text = "Delete set",
                            onClick = viewModel::deleteSelectedSet,
                            enabled = uiState.sets.isNotEmpty(),
                        )
                    }
                    if (uiState.sets.isEmpty()) {
                        Text(
                            text = "Create a set, then tap notes on the keyboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EditorActionChip(
                            text = "Clear selected set",
                            onClick = viewModel::clearSelectedSet,
                            enabled = uiState.selectedSet?.sounds?.isNotEmpty() == true,
                        )
                    }
                }

                SkalesPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkalesSecondaryButton(text = "Cancel", onClick = onNavigateBack)
                        SkalesPrimaryButton(
                            text = "Save scale",
                            onClick = { viewModel.saveScale(onNavigateBack) },
                            enabled = uiState.name.isNotBlank() && uiState.sets.any { it.sounds.isNotEmpty() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
        ),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        label = { Text(text) },
    )
}

@Composable
private fun SetSelectorPill(
    setIndex: Int,
    set: ScaleSet,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val noteCount = set.sounds.count { it.kind == ScaleSoundKind.Note }
    val cueCount = set.sounds.count { it.kind == ScaleSoundKind.Cue }
    AssistChip(
        onClick = onSelect,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
        ),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        label = {
            Text("Set ${setIndex + 1}  $noteCount notes${if (cueCount > 0) "  $cueCount cue" else ""}")
        },
    )
}

@Composable
private fun SetCard(
    setIndex: Int,
    set: ScaleSet,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    SkalesPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Set ${setIndex + 1}${set.breakAfterBeats?.let { "  (+${it} beat break)" } ?: ""}",
                style = MaterialTheme.typography.titleSmall,
            )
            SkalesPill(text = if (isSelected) "Selected" else "Tap to select", highlighted = isSelected)
        }
        EditorActionChip(
            text = if (isSelected) "Selected" else "Select set",
            onClick = onSelect,
            enabled = !isSelected,
        )
        if (set.sounds.isEmpty()) {
            Text(
                text = "Empty set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                set.sounds.forEachIndexed { soundIndex, sound ->
                    val prefix = if (sound.kind == ScaleSoundKind.Cue) "C" else "N"
                    AssistChip(
                        onClick = onSelect,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                            },
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        label = {
                            Text("${prefix}${soundIndex + 1}: ${ScaleEditorOps.labelForSound(sound)}")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorActionChip(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
        ),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        label = { Text(text) },
    )
}

private fun playbackCursorLabel(cursor: PlaybackCursor): String {
    return if (cursor.isFinished) {
        "Cursor: finished"
    } else {
        "Cursor: set ${cursor.setIndex + 1}, sound ${cursor.soundIndexInSet + 1}"
    }
}
