package com.example.skales.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.app.components.PianoKeyboard
import com.example.skales.app.components.SetPianoRollEditor
import com.example.skales.app.components.SkalesBackground
import com.example.skales.app.components.SkalesCircleButton
import com.example.skales.app.components.SkalesPanel
import com.example.skales.app.components.SkalesPill
import com.example.skales.app.components.SkalesPrimaryButton
import com.example.skales.app.components.SkalesSecondaryButton
import com.example.skales.app.components.SkalesSectionHeader
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
    var dockMode by rememberSaveable { mutableStateOf(EditorDockMode.Playback) }
    val canSave = uiState.isLoaded && uiState.name.isNotBlank() && uiState.sets.any { it.sounds.isNotEmpty() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(if (uiState.isEditing) "Edit scale" else "New scale") },
                navigationIcon = {
                    SkalesSecondaryButton(text = "Back", onClick = onNavigateBack)
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveScale(onNavigateBack) },
                        enabled = canSave,
                    ) {
                        Text(
                            text = "Save",
                            color = if (canSave) SaveActionColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.isLoaded) {
                EditorPlaybackDock(
                    mode = dockMode,
                    onModeChange = { dockMode = it },
                    bpm = uiState.bpm,
                    cursorLabel = playbackCursorLabel(uiState.playbackCursor),
                    isPlaying = uiState.isPlaying,
                    canPlay = uiState.sets.any { it.sounds.isNotEmpty() } && !uiState.isPlaying,
                    canStep = uiState.sets.any { it.sounds.isNotEmpty() } && !uiState.isPlaying,
                    onDecreaseBpm = viewModel::decreaseBpm,
                    onIncreaseBpm = viewModel::increaseBpm,
                    onPlay = viewModel::playScale,
                    onStep = viewModel::stepScale,
                    onReset = viewModel::resetPlaybackCursor,
                    onStop = viewModel::stopScale,
                    onNotePressed = viewModel::onNotePressed,
                )
            }
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
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                }

                SkalesPanel {
                    SkalesSectionHeader(
                        title = "Piano roll",
                        supporting = "Tap a key to arm a pitch, tap empty grid space to place it, drag blocks to change pitch or spacing, and scroll vertically for more octaves.",
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
                        title = "Sets",
                        supporting = "Keep one set selected while adding notes or cue sounds.",
                    )
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

            }
        }
    }
}

private val SaveActionColor = Color(0xFF2E7D32)

@Composable
private fun EditorPlaybackDock(
    mode: EditorDockMode,
    onModeChange: (EditorDockMode) -> Unit,
    bpm: Int,
    cursorLabel: String,
    isPlaying: Boolean,
    canPlay: Boolean,
    canStep: Boolean,
    onDecreaseBpm: () -> Unit,
    onIncreaseBpm: () -> Unit,
    onPlay: () -> Unit,
    onStep: () -> Unit,
    onReset: () -> Unit,
    onStop: () -> Unit,
    onNotePressed: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), thickness = 1.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DockModeChip(
                        text = "Playback",
                        selected = mode == EditorDockMode.Playback,
                        onClick = { onModeChange(EditorDockMode.Playback) },
                    )
                    DockModeChip(
                        text = "Keyboard",
                        selected = mode == EditorDockMode.Keyboard,
                        onClick = { onModeChange(EditorDockMode.Keyboard) },
                    )
                }

                if (mode == EditorDockMode.Playback) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = "Playback", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = cursorLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$bpm BPM",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            SkalesCircleButton(label = "-", onClick = onDecreaseBpm, size = 42.dp)
                            SkalesCircleButton(label = "+", onClick = onIncreaseBpm, size = 42.dp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaybackTransportButton(
                            isStop = isPlaying,
                            onClick = if (isPlaying) onStop else onPlay,
                            enabled = if (isPlaying) true else canPlay,
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        SkalesSecondaryButton(text = "Step", onClick = onStep, enabled = canStep)
                        SkalesSecondaryButton(text = "Reset", onClick = onReset)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                } else {
                    Text(
                        text = "Keyboard",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    PianoKeyboard(
                        onNotePressed = onNotePressed,
                        modifier = Modifier.fillMaxWidth(),
                        whiteKeyHeight = 160.dp,
                        blackKeyHeight = 96.dp,
                    )
                }
            }
        }
    }
}

private enum class EditorDockMode {
    Playback,
    Keyboard,
}

@Composable
private fun PlaybackTransportButton(
    isStop: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                },
                shape = CircleShape,
            )
            .background(containerColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            if (isStop) {
                val inset = size.minDimension * 0.2f
                drawRect(
                    color = contentColor,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - (inset * 2), size.height - (inset * 2)),
                )
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.18f)
                    lineTo(size.width * 0.28f, size.height * 0.82f)
                    lineTo(size.width * 0.8f, size.height * 0.5f)
                    close()
                }
                drawPath(path = path, color = contentColor)
            }
        }
    }
}

@Composable
private fun DockModeChip(
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
    SkalesPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
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
