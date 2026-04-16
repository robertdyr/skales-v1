package com.example.skales.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.skales.editor.SetGrid
import com.example.skales.editor.SetGridNote
import com.example.skales.model.Note
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val CellWidth = 36.dp
private val RowHeight = 28.dp
private val LabelWidth = 52.dp
private val NoteWidth = 30.dp
private val NoteHeight = 20.dp
private const val DefaultFocusMidi = 60
private const val VisibleRowCount = 18

@Composable
fun SetPianoRollEditor(
    grid: SetGrid,
    onCellTap: (column: Int, midi: Int) -> Unit,
    onNoteMove: (soundId: String, midi: Int, column: Int) -> Unit,
    onDeleteNote: (soundId: String) -> Unit,
    modifier: Modifier = Modifier,
    controls: @Composable (() -> Unit)? = null,
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current
    val rowCount = grid.maxMidi - grid.minMidi + 1
    val rollWidth = CellWidth * grid.columnCount
    val rollHeight = RowHeight * rowCount
    val viewportHeight = RowHeight * VisibleRowCount
    var selectedSoundId by remember(grid.notes) { mutableStateOf<String?>(null) }
    val dragOffsets = remember { mutableStateMapOf<String, Offset>() }
    val strongGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
    val softGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    val rowGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val focusedMidi = remember(grid.notes, grid.minMidi, grid.maxMidi) {
        val lowestNote = grid.notes.minOfOrNull { it.midi }
        val highestNote = grid.notes.maxOfOrNull { it.midi }
        val midpoint = if (lowestNote != null && highestNote != null) {
            (lowestNote + highestNote) / 2
        } else {
            DefaultFocusMidi
        }
        midpoint.coerceIn(grid.minMidi, grid.maxMidi)
    }

    LaunchedEffect(focusedMidi, rowCount) {
        val maxScroll = with(density) {
            max(0, rollHeight.roundToPx() - viewportHeight.roundToPx())
        }
        val targetRow = grid.maxMidi - focusedMidi
        val centeredRow = max(0, targetRow - (VisibleRowCount / 2))
        val targetScroll = min(maxScroll, with(density) { (RowHeight * centeredRow).roundToPx() })
        verticalScroll.scrollTo(targetScroll)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            controls?.invoke()
            IconButton(
                onClick = {
                    selectedSoundId?.let(onDeleteNote)
                    selectedSoundId = null
                },
                enabled = selectedSoundId != null,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete selected note",
                    tint = if (selectedSoundId != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll),
        ) {
            Box(
                modifier = Modifier
                    .width(LabelWidth)
                    .height(viewportHeight)
                    .verticalScroll(verticalScroll),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rollHeight),
                ) {
                    for ((rowIndex, midi) in (grid.maxMidi downTo grid.minMidi).withIndex()) {
                        Box(
                            modifier = Modifier
                                .offset(y = RowHeight * rowIndex)
                                .width(LabelWidth)
                                .height(RowHeight)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = noteLabel(midi),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(rollWidth)
                    .height(viewportHeight)
                    .verticalScroll(verticalScroll),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rollHeight)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
                        .pointerInput(grid) {
                            detectTapGestures(
                                onTap = { start ->
                                    val column = (start.x / CellWidth.toPx()).toInt().coerceIn(0, grid.columnCount - 1)
                                    val row = (start.y / RowHeight.toPx()).toInt().coerceIn(0, rowCount - 1)
                                    val midi = grid.maxMidi - row
                                    val hit = findHitNote(start, grid)
                                    if (hit == null) {
                                        onCellTap(column, midi)
                                    } else {
                                        selectedSoundId = hit.soundId
                                    }
                                },
                            )
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        repeat(grid.columnCount + 1) { column ->
                            val x = column * CellWidth.toPx()
                            drawLine(
                                color = if (column % 4 == 0) strongGridColor else softGridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f,
                            )
                        }
                        repeat(rowCount + 1) { row ->
                            val y = row * RowHeight.toPx()
                            drawLine(
                                color = rowGridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f,
                            )
                        }
                    }

                    grid.notes.forEach { note ->
                        val baseX = (CellWidth * note.column) + ((CellWidth - NoteWidth) / 2)
                        val baseY = (RowHeight * (grid.maxMidi - note.midi)) + ((RowHeight - NoteHeight) / 2)
                        val dragOffset = dragOffsets[note.soundId] ?: Offset.Zero
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = baseX.roundToPx() + dragOffset.x.roundToInt(),
                                        y = baseY.roundToPx() + dragOffset.y.roundToInt(),
                                    )
                                }
                                .width(NoteWidth)
                                .height(NoteHeight)
                                .background(
                                    color = if (selectedSoundId == note.soundId) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .pointerInput(note) {
                                    detectTapGestures(
                                        onTap = {
                                            selectedSoundId = note.soundId
                                        },
                                    )
                                }
                                .pointerInput(note, grid) {
                                    detectDragGestures(
                                        onDragStart = {
                                            selectedSoundId = note.soundId
                                        },
                                        onDragCancel = {
                                            dragOffsets.remove(note.soundId)
                                        },
                                        onDragEnd = {
                                            val offset = dragOffsets.remove(note.soundId) ?: Offset.Zero
                                            val movedColumns = (offset.x / CellWidth.toPx()).roundToInt()
                                            val movedRows = (offset.y / RowHeight.toPx()).roundToInt()
                                            onNoteMove(
                                                note.soundId,
                                                (note.midi - movedRows).coerceIn(grid.minMidi, grid.maxMidi),
                                                (note.column + movedColumns).coerceAtLeast(0),
                                            )
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsets[note.soundId] = (dragOffsets[note.soundId] ?: Offset.Zero) + dragAmount
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = noteLabel(note.midi),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun findHitNote(position: Offset, grid: SetGrid): SetGridNote? {
    return grid.notes.lastOrNull { note ->
        val left = (note.column * CellWidth.value) + ((CellWidth.value - NoteWidth.value) / 2f)
        val top = ((grid.maxMidi - note.midi) * RowHeight.value) + ((RowHeight.value - NoteHeight.value) / 2f)
        val right = left + NoteWidth.value
        val bottom = top + NoteHeight.value
        position.x in left..right && position.y in top..bottom
    }
}

private fun noteLabel(midi: Int): String {
    val note = Note.fromMidi(midi)
    return "${note.name}${note.octave}"
}
