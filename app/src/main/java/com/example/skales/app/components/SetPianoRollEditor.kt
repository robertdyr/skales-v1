package com.example.skales.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.skales.editor.SetGrid
import com.example.skales.editor.SetGridNote
import com.example.skales.model.Note
import com.example.skales.model.ScaleSoundKind
import kotlin.math.roundToInt

private val TimeRowHeight = 36.dp
private val NoteWidth = 32.dp
private val NoteHeight = 28.dp
private val CueSize = 28.dp
private val CueCircleSpacing = 6.dp
private val BoundaryHandleHeight = 2.dp
private val BoundaryGripWidth = 22.dp
private val BoundaryGripHeight = 12.dp
private val BoundaryVisualHeight = 20.dp
private val BoundaryAccentColor = Color(0xFFD85C5C)

@Composable
fun SetPianoRollEditor(
    grid: SetGrid,
    selectedSetIndex: Int,
    onSelectSet: (Int) -> Unit,
    onCellTap: (column: Int, midi: Int) -> Unit,
    onNoteMove: (soundId: String, midi: Int, column: Int) -> Unit,
    onBoundaryMove: (targetSetIndex: Int, column: Int) -> Unit,
    onDeleteNote: (soundId: String) -> Unit,
    modifier: Modifier = Modifier,
    pitchScrollState: ScrollState = rememberScrollState(),
    controls: @Composable (() -> Unit)? = null,
) {
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current
    val timeRowHeightPx = with(density) { TimeRowHeight.toPx() }
    val noteWidthPx = with(density) { NoteWidth.toPx() }
    val noteHeightPx = with(density) { NoteHeight.toPx() }
    val cueSizePx = with(density) { CueSize.toPx() }
    val cueSpacingPx = with(density) { CueCircleSpacing.toPx() }
    val layoutKeys = remember(grid.minMidi, grid.maxMidi) {
        PianoLayout.keys().filter { it.midi in grid.minMidi..grid.maxMidi }
    }
    val layoutKeyBounds = remember(layoutKeys, density) {
        layoutKeys.map { key ->
            PianoLayoutKeyBounds(
                midi = key.midi,
                isBlackKey = key.note.isBlackKey,
                leftPx = with(density) { key.left.toPx() },
                widthPx = with(density) { key.width.toPx() },
                centerPx = with(density) { key.center.toPx() },
            )
        }
    }
    val rollWidth = remember(layoutKeys) { PianoLayout.totalWidth(layoutKeys) }
    val rollHeight = TimeRowHeight * grid.columnCount
    var selectedSoundId by remember(grid.notes) { mutableStateOf<String?>(null) }
    var didInitializeVerticalScroll by remember { mutableStateOf(false) }
    val dragOffsets = remember { mutableStateMapOf<String, Offset>() }
    val boundaryDragOffsets = remember { mutableStateMapOf<Int, Float>() }
    val pitchGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val strongTimeGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
    val softTimeGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val selectedSetColor = MaterialTheme.colorScheme.primary
    val unselectedSetColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.62f)
    val setBoundaryColor = BoundaryAccentColor
    val cueSelectedColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
    val cueUnselectedColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.52f)
    val selectedSetStartColumn = grid.setStartColumns.getOrElse(selectedSetIndex) { 0 }
    val selectedSetEndColumnExclusive = grid.setStartColumns
        .getOrNull(selectedSetIndex + 1)
        ?: grid.columnCount

    LaunchedEffect(verticalScroll.maxValue, didInitializeVerticalScroll) {
        if (!didInitializeVerticalScroll && verticalScroll.maxValue > 0) {
            verticalScroll.scrollTo(verticalScroll.maxValue)
            didInitializeVerticalScroll = true
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val viewportHeight = maxHeight
        val viewportWidth = maxWidth

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .horizontalScroll(pitchScrollState),
            ) {
                Box(
                    modifier = Modifier
                        .width(rollWidth)
                        .fillMaxHeight()
                        .verticalScroll(verticalScroll),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxOf(rollHeight, viewportHeight))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f))
                            .pointerInput(grid) {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        val midi = findMidiAtX(tapOffset.x, layoutKeyBounds)
                                        val rowFromTop = (tapOffset.y / timeRowHeightPx).toInt()
                                            .coerceIn(0, grid.columnCount - 1)
                                        val globalColumn = (grid.columnCount - 1 - rowFromTop)
                                            .coerceIn(0, grid.columnCount - 1)
                                        val hit = findHitNote(tapOffset, grid, layoutKeyBounds, timeRowHeightPx, noteWidthPx, noteHeightPx, cueSizePx)
                                        if (hit == null) {
                                            if (globalColumn in selectedSetStartColumn until selectedSetEndColumnExclusive) {
                                                onCellTap(globalColumn - selectedSetStartColumn, midi)
                                            }
                                        }
                                    },
                                )
                            },
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            layoutKeys.forEach { key ->
                                val x = key.left.toPx()
                                drawLine(
                                    color = pitchGridColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1f,
                                )
                            }
                            drawLine(
                                color = pitchGridColor,
                                start = Offset(rollWidth.toPx(), 0f),
                                end = Offset(rollWidth.toPx(), size.height),
                                strokeWidth = 1f,
                            )
                            for (timeIndex in 0..grid.columnCount) {
                                val y = (grid.columnCount - timeIndex) * TimeRowHeight.toPx()
                                drawLine(
                                    color = if (timeIndex % 4 == 0) strongTimeGridColor else softTimeGridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                )
                            }
                            grid.setStartColumns.drop(1).forEach { startColumn ->
                                val y = (grid.columnCount - startColumn) * TimeRowHeight.toPx()
                                drawLine(
                                    color = setBoundaryColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 2f,
                                )
                            }
                        }

                        grid.notes.forEach { note ->
                            val dragOffset = dragOffsets[note.soundId] ?: Offset.Zero
                            val isCue = note.kind == ScaleSoundKind.Cue
                            val noteMidis = note.midis.sorted()
                            if (noteMidis.isEmpty()) return@forEach
                            val noteKeys = noteMidis.mapNotNull { midi -> layoutKeys.firstOrNull { it.midi == midi } }
                            if (noteKeys.isEmpty()) return@forEach
                            val primaryMidi = displayMidi(noteMidis)
                            val primaryKey = layoutKeys.firstOrNull { it.midi == primaryMidi } ?: noteKeys[noteKeys.lastIndex / 2]
                            val itemWidth = if (isCue) CueSize else NoteWidth
                            val itemHeight = if (isCue) CueSize else NoteHeight
                            val noteColor = when {
                                isCue && note.setIndex == selectedSetIndex -> cueSelectedColor
                                isCue -> cueUnselectedColor
                                selectedSoundId == note.soundId -> selectedSetColor
                                note.setIndex == selectedSetIndex -> selectedSetColor.copy(alpha = 0.82f)
                                else -> unselectedSetColor
                            }
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (primaryKey.center - (itemWidth / 2)).roundToPx() + dragOffset.x.roundToInt(),
                                            y = ((TimeRowHeight * (grid.columnCount - note.column - 1)) + ((TimeRowHeight - itemHeight) / 2)).roundToPx() + dragOffset.y.roundToInt(),
                                        )
                                    }
                                    .width(groupWidth(noteKeys = noteKeys, isCue = isCue, itemWidth = itemWidth, cueSpacing = CueCircleSpacing))
                                    .height(itemHeight)
                                    .let { baseModifier ->
                                        baseModifier
                                            .pointerInput(note, selectedSetIndex) {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (note.setIndex != selectedSetIndex) {
                                                            onSelectSet(note.setIndex)
                                                        }
                                                        selectedSoundId = note.soundId
                                                    },
                                                )
                                            }
                                            .let { tappableModifier ->
                                                if (note.setIndex == selectedSetIndex) {
                                                    tappableModifier.pointerInput(note, grid) {
                                                        detectDragGestures(
                                                            onDragStart = { selectedSoundId = note.soundId },
                                                            onDragCancel = { dragOffsets.remove(note.soundId) },
                                                            onDragEnd = {
                                                                val offset = dragOffsets.remove(note.soundId) ?: Offset.Zero
                                                                val movedRows = (offset.y / timeRowHeightPx).roundToInt()
                                                                val movedMidi = findMidiAtX(
                                                                    x = with(density) { primaryKey.center.toPx() } + offset.x,
                                                                    layoutKeys = layoutKeyBounds,
                                                                )
                                                                val targetGlobalColumn = (note.column - movedRows).coerceAtLeast(0)
                                                                onNoteMove(
                                                                    note.soundId,
                                                                    movedMidi,
                                                                    targetGlobalColumn,
                                                                )
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsets[note.soundId] = (dragOffsets[note.soundId] ?: Offset.Zero) + dragAmount
                                                            },
                                                        )
                                                    }
                                                } else {
                                                    tappableModifier
                                                }
                                            }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCue) {
                                    CueGroup(
                                        noteMidis = noteMidis,
                                        noteKeys = noteKeys,
                                        primaryKey = primaryKey,
                                        color = noteColor,
                                        circleSize = CueSize,
                                    )
                                } else {
                                    NoteGroup(
                                        noteKeys = noteKeys,
                                        primaryKey = primaryKey,
                                        color = noteColor,
                                    )
                                }
                            }
                        }

                        val viewportGripOffset = with(density) { pitchScrollState.value.toDp() } + ((viewportWidth - BoundaryGripWidth) / 2)

                        grid.setStartColumns.drop(1).forEachIndexed { index, startColumn ->
                            val targetSetIndex = index + 1
                            val boundaryDragOffset = boundaryDragOffsets[targetSetIndex] ?: 0f
                            Box(
                                modifier = Modifier
                                    .offset(y = (TimeRowHeight * (grid.columnCount - startColumn)) - (BoundaryVisualHeight / 2))
                                    .offset { IntOffset(x = 0, y = boundaryDragOffset.roundToInt()) }
                                    .width(rollWidth)
                                    .height(BoundaryVisualHeight)
                                    .zIndex(1f)
                                    .pointerInput(targetSetIndex, startColumn, timeRowHeightPx) {
                                        var accumulatedDragY = 0f
                                        detectDragGestures(
                                            onDragStart = {
                                                accumulatedDragY = 0f
                                                boundaryDragOffsets[targetSetIndex] = 0f
                                            },
                                            onDragCancel = {
                                                accumulatedDragY = 0f
                                                boundaryDragOffsets.remove(targetSetIndex)
                                            },
                                            onDragEnd = {
                                                val movedRows = (accumulatedDragY / timeRowHeightPx).roundToInt()
                                                val targetColumn = (startColumn - movedRows).coerceAtLeast(0)
                                                onBoundaryMove(targetSetIndex, targetColumn)
                                                accumulatedDragY = 0f
                                                boundaryDragOffsets.remove(targetSetIndex)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                accumulatedDragY += dragAmount.y
                                                boundaryDragOffsets[targetSetIndex] = accumulatedDragY
                                            },
                                        )
                                    },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxWidth()
                                        .height(BoundaryHandleHeight)
                                        .background(setBoundaryColor.copy(alpha = 0.9f)),
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = viewportGripOffset)
                                        .width(BoundaryGripWidth)
                                        .height(BoundaryGripHeight)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = setBoundaryColor,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(6.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            controls?.invoke()
            IconButton(
                onClick = {
                    selectedSoundId?.let(onDeleteNote)
                    selectedSoundId = null
                },
                enabled = selectedSoundId != null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
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
    }
}

private fun findHitNote(
    position: Offset,
    grid: SetGrid,
    layoutKeys: List<PianoLayoutKeyBounds>,
    timeRowHeightPx: Float,
    noteWidthPx: Float,
    noteHeightPx: Float,
    cueSizePx: Float,
): SetGridNote? {
    return grid.notes.lastOrNull { note ->
        val noteMidis = note.midis.sorted()
        if (noteMidis.isEmpty()) return@lastOrNull false
        val noteKeys = noteMidis.mapNotNull { midi -> layoutKeys.firstOrNull { it.midi == midi } }
        if (noteKeys.isEmpty()) return@lastOrNull false
        val itemHeightPx = if (note.kind == ScaleSoundKind.Cue) cueSizePx else noteHeightPx
        val top = ((grid.columnCount - note.column - 1) * timeRowHeightPx) + ((timeRowHeightPx - itemHeightPx) / 2f)
        val bottom = top + itemHeightPx
        noteKeys.any { key ->
            val itemWidthPx = if (note.kind == ScaleSoundKind.Cue) cueSizePx else noteWidthPx
            val rect = Rect(
                left = key.centerPx - (itemWidthPx / 2f),
                top = top,
                right = key.centerPx + (itemWidthPx / 2f),
                bottom = bottom,
            )
            position.x in rect.left..rect.right && position.y in rect.top..rect.bottom
        }
    }
}

private fun findMidiAtX(x: Float, layoutKeys: List<PianoLayoutKeyBounds>): Int {
    val blackHit = layoutKeys
        .filter { it.isBlackKey }
        .lastOrNull { x in it.leftPx..(it.leftPx + it.widthPx) }
    if (blackHit != null) return blackHit.midi

    val whiteHit = layoutKeys
        .filterNot { it.isBlackKey }
        .lastOrNull { x in it.leftPx..(it.leftPx + it.widthPx) }
    if (whiteHit != null) return whiteHit.midi

    return layoutKeys.minByOrNull { kotlin.math.abs(it.centerPx - x) }?.midi ?: layoutKeys.first().midi
}

private data class PianoLayoutKeyBounds(
    val midi: Int,
    val isBlackKey: Boolean,
    val leftPx: Float,
    val widthPx: Float,
    val centerPx: Float,
)

private fun noteBodyLabel(midi: Int): String {
    val note = Note.fromMidi(midi)
    return note.name
}

@Composable
private fun CueGroup(
    noteMidis: List<Int>,
    noteKeys: List<PianoLayoutKey>,
    primaryKey: PianoLayoutKey,
    color: Color,
    circleSize: androidx.compose.ui.unit.Dp,
) {
    val isSingleNoteCue = noteKeys.size == 1 && noteMidis.size == 1
    noteKeys.forEach { key ->
        Box(
            modifier = Modifier
                .offset(x = key.center - primaryKey.center)
                .width(circleSize)
                .height(circleSize)
                .background(color = color, shape = androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSingleNoteCue) {
                Text(
                    text = noteBodyLabel(noteMidis.first()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun NoteGroup(
    noteKeys: List<PianoLayoutKey>,
    primaryKey: PianoLayoutKey,
    color: Color,
) {
    val key = noteKeys.first()
    Box(
        modifier = Modifier
            .offset(x = key.center - primaryKey.center)
            .width(NoteWidth)
            .height(NoteHeight)
            .background(color = color, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = noteBodyLabel(key.midi),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun displayMidi(midis: List<Int>): Int = midis.average().roundToInt()

private fun groupWidth(
    noteKeys: List<PianoLayoutKey>,
    isCue: Boolean,
    itemWidth: androidx.compose.ui.unit.Dp,
    cueSpacing: androidx.compose.ui.unit.Dp,
): androidx.compose.ui.unit.Dp {
    if (noteKeys.isEmpty()) return itemWidth
    if (!isCue || noteKeys.size == 1) return itemWidth
    return (noteKeys.last().center - noteKeys.first().center) + itemWidth + cueSpacing
}
