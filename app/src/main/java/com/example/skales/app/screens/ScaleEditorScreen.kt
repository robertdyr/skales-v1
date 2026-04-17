package com.example.skales.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.app.components.PianoKeyboard
import com.example.skales.app.components.SetPianoRollEditor
import com.example.skales.app.components.SkalesBackground
import com.example.skales.app.components.SkalesCircleButton
import com.example.skales.app.components.SkalesPanel
import com.example.skales.app.components.SkalesSecondaryButton
import com.example.skales.app.viewmodel.ScaleEditorViewModel
import com.example.skales.editor.SetGridOps
import com.example.skales.model.ScaleSet
import com.example.skales.player.PlaybackCursor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleEditorScreen(
    viewModel: ScaleEditorViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canSave = uiState.isLoaded && uiState.name.isNotBlank() && uiState.sets.any { it.sounds.isNotEmpty() }
    val pitchScrollState = rememberScrollState()
    var isPlaybackPanelExpanded by remember { mutableStateOf(false) }
    var isGridPanelExpanded by remember { mutableStateOf(false) }
    var isSetsPanelExpanded by remember { mutableStateOf(false) }

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
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)) {
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

                val selectedSet = uiState.selectedSet ?: ScaleSet(sounds = emptyList())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        SetPianoRollEditor(
                            grid = SetGridOps.toGrid(uiState.sets, stepBeats = uiState.snapStepBeats),
                            selectedSetIndex = uiState.selectedSetIndex,
                            onSelectSet = viewModel::selectSet,
                            onCellTap = viewModel::addNoteToSelectedSetAtPosition,
                            onNoteMove = viewModel::moveNoteInSelectedSet,
                            onBoundaryMove = viewModel::moveSetBoundary,
                            onDeleteNote = viewModel::removeNoteFromSelectedSet,
                            pitchScrollState = pitchScrollState,
                            modifier = Modifier.fillMaxSize(),
                        )
                        PlaybackOverlay(
                            bpm = uiState.bpm,
                            cursor = uiState.playbackCursor,
                            isPlaying = uiState.isPlaying,
                            canPlay = uiState.sets.any { it.sounds.isNotEmpty() },
                            isExpanded = isPlaybackPanelExpanded,
                            onToggleExpanded = { isPlaybackPanelExpanded = !isPlaybackPanelExpanded },
                            onPlayToggle = {
                                if (uiState.isPlaying) viewModel.stopScale() else viewModel.playScale()
                            },
                            onStep = viewModel::stepScale,
                            onReset = viewModel::resetPlaybackCursor,
                            onDecreaseBpm = viewModel::decreaseBpm,
                            onIncreaseBpm = viewModel::increaseBpm,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp, bottom = 80.dp),
                        )
                        GridOverlay(
                            selectedStepBeats = uiState.snapStepBeats,
                            isExpanded = isGridPanelExpanded,
                            onToggleExpanded = { isGridPanelExpanded = !isGridPanelExpanded },
                            onSelectStepBeats = viewModel::setSnapStepBeats,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp, top = 80.dp),
                        )
                        SetsOverlay(
                            sets = uiState.sets,
                            selectedSetIndex = uiState.selectedSetIndex,
                            isExpanded = isSetsPanelExpanded,
                            canRemovePreCue = selectedSet.sounds.firstOrNull()?.kind == com.example.skales.model.ScaleSoundKind.Cue,
                            canRemovePostCue = selectedSet.sounds.lastOrNull()?.kind == com.example.skales.model.ScaleSoundKind.Cue,
                            canClearSelectedSet = selectedSet.sounds.isNotEmpty(),
                            onToggleExpanded = { isSetsPanelExpanded = !isSetsPanelExpanded },
                            onSelectSet = viewModel::selectSet,
                            onAddSet = viewModel::addSet,
                            onDeleteSelectedSet = viewModel::deleteSelectedSet,
                            onClearSelectedSet = viewModel::clearSelectedSet,
                            onAddPreCue = viewModel::addChordPreCueToSelectedSet,
                            onAddPostCue = viewModel::addChordPostCueToSelectedSet,
                            onRemovePreCue = viewModel::removeChordPreCueFromSelectedSet,
                            onRemovePostCue = viewModel::removeChordPostCueFromSelectedSet,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp),
                        )
                    }
                    PianoKeyboard(
                        onNotePressed = viewModel::onNotePressed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        whiteKeyHeight = 188.dp,
                        blackKeyHeight = 116.dp,
                        scrollState = pitchScrollState,
                    )
                }
            }
        }
    }
}

private val SaveActionColor = Color(0xFF2E7D32)

@Composable
private fun PlaybackOverlay(
    bpm: Int,
    cursor: PlaybackCursor,
    isPlaying: Boolean,
    canPlay: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPlayToggle: () -> Unit,
    onStep: () -> Unit,
    onReset: () -> Unit,
    onDecreaseBpm: () -> Unit,
    onIncreaseBpm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isExpanded) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = playbackCursorLabel(cursor),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$bpm BPM",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        SkalesCircleButton(label = "-", onClick = onDecreaseBpm, size = 40.dp)
                        SkalesCircleButton(label = "+", onClick = onIncreaseBpm, size = 40.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverlayActionChip(text = "Step", onClick = onStep, enabled = !isPlaying && canPlay)
                        OverlayActionChip(text = "Reset", onClick = onReset)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SkalesCircleButton(
                label = if (isExpanded) "×" else "···",
                onClick = onToggleExpanded,
                size = 48.dp,
                highlighted = isExpanded,
            )
            SkalesCircleButton(
                label = if (isPlaying) "||" else ">",
                onClick = onPlayToggle,
                size = 62.dp,
                highlighted = isPlaying,
                enabled = canPlay || isPlaying,
            )
        }
    }
}

@Composable
private fun OverlayActionChip(
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
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        label = { Text(text) },
    )
}

private fun playbackCursorLabel(cursor: PlaybackCursor): String {
    return if (cursor.isFinished) {
        "Finished"
    } else {
        "Set ${cursor.setIndex + 1}, sound ${cursor.soundIndexInSet + 1}"
    }
}

@Composable
private fun GridOverlay(
    selectedStepBeats: Float,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectStepBeats: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isExpanded) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Grid",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GridStepChip(
                            text = "1/1",
                            selected = selectedStepBeats == SetGridOps.CoarseStepBeats,
                            onClick = { onSelectStepBeats(SetGridOps.CoarseStepBeats) },
                        )
                        GridStepChip(
                            text = "1/2",
                            selected = selectedStepBeats == SetGridOps.DefaultStepBeats,
                            onClick = { onSelectStepBeats(SetGridOps.DefaultStepBeats) },
                        )
                        GridStepChip(
                            text = "1/4",
                            selected = selectedStepBeats == SetGridOps.FineStepBeats,
                            onClick = { onSelectStepBeats(SetGridOps.FineStepBeats) },
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkalesCircleButton(
                label = if (isExpanded) "×" else "#",
                onClick = onToggleExpanded,
                size = 48.dp,
                highlighted = isExpanded,
            )
        }
    }
}

@Composable
private fun GridStepChip(
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
            labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ),
        label = { Text(text) },
    )
}

@Composable
private fun SetsOverlay(
    sets: List<ScaleSet>,
    selectedSetIndex: Int,
    isExpanded: Boolean,
    canRemovePreCue: Boolean,
    canRemovePostCue: Boolean,
    canClearSelectedSet: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectSet: (Int) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSelectedSet: () -> Unit,
    onClearSelectedSet: () -> Unit,
    onAddPreCue: () -> Unit,
    onAddPostCue: () -> Unit,
    onRemovePreCue: () -> Unit,
    onRemovePostCue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isExpanded) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 240.dp, max = 300.dp)
                        .heightIn(max = 380.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Sets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverlayActionChip(text = "New", onClick = onAddSet)
                        OverlayActionChip(text = "Delete", onClick = onDeleteSelectedSet, enabled = sets.size > 1)
                        OverlayActionChip(text = "Clear", onClick = onClearSelectedSet, enabled = canClearSelectedSet)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverlayActionChip(
                            text = if (canRemovePreCue) "Remove pre" else "Add pre",
                            onClick = if (canRemovePreCue) onRemovePreCue else onAddPreCue,
                        )
                        OverlayActionChip(
                            text = if (canRemovePostCue) "Remove post" else "Add post",
                            onClick = if (canRemovePostCue) onRemovePostCue else onAddPostCue,
                        )
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(sets) { index, set ->
                            SetListItem(
                                setIndex = index,
                                set = set,
                                selected = index == selectedSetIndex,
                                onClick = { onSelectSet(index) },
                            )
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkalesCircleButton(
                label = if (isExpanded) "×" else "S",
                onClick = onToggleExpanded,
                size = 48.dp,
                highlighted = isExpanded,
            )
        }
    }
}

@Composable
private fun SetListItem(
    setIndex: Int,
    set: ScaleSet,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val noteCount = set.sounds.count { it.kind == com.example.skales.model.ScaleSoundKind.Note }
    val cueCount = set.sounds.count { it.kind == com.example.skales.model.ScaleSoundKind.Cue }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Set ${setIndex + 1}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$noteCount notes${if (cueCount > 0) "  $cueCount cue" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = setSummary(set),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

private fun setSummary(set: ScaleSet): String {
    if (set.sounds.isEmpty()) return "Empty set"
    return set.sounds.take(4).joinToString("  ") { sound ->
        val prefix = if (sound.kind == com.example.skales.model.ScaleSoundKind.Cue) "Cue" else "N"
        "$prefix ${com.example.skales.editor.ScaleEditorOps.labelForSound(sound)}"
    }
}
