package com.example.skales.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.skales.model.Note

private data class PianoKey(
    val midi: Int,
    val note: Note,
)

private const val KeyboardStartMidi = 21
private const val KeyboardEndMidi = 108
private const val InitialVisibleMidi = 48
private val WhiteKeyWidth = 44.dp
private val BlackKeyWidth = 28.dp

@Composable
fun PianoKeyboard(
    onNotePressed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allKeys = (KeyboardStartMidi..KeyboardEndMidi).map { midi -> PianoKey(midi = midi, note = Note.fromMidi(midi)) }
    val whiteKeys = allKeys.filterNot { it.note.isBlackKey }
    val blackKeys = allKeys.filter { it.note.isBlackKey }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val initialWhiteKeyIndex = remember(whiteKeys) {
        whiteKeys.indexOfFirst { it.midi == InitialVisibleMidi }.coerceAtLeast(0)
    }

    LaunchedEffect(initialWhiteKeyIndex) {
        val initialOffsetPx = with(density) { (WhiteKeyWidth * initialWhiteKeyIndex).roundToPx() }
        scrollState.scrollTo(initialOffsetPx)
    }

    Box(
        modifier = modifier.horizontalScroll(scrollState),
    ) {
        Box(modifier = Modifier.width(WhiteKeyWidth * whiteKeys.size)) {
            Row {
                whiteKeys.forEach { key ->
                    WhiteKey(
                        note = key.note,
                        width = WhiteKeyWidth,
                        onClick = { onNotePressed(key.midi) },
                    )
                }
            }

            blackKeys.forEach { key ->
                val offset = blackKeyOffset(key.midi, whiteKeys, WhiteKeyWidth, BlackKeyWidth)
                BlackKey(
                    width = BlackKeyWidth,
                    modifier = Modifier.offset(x = offset),
                    onClick = { onNotePressed(key.midi) },
                )
            }
        }
    }
}

@Composable
private fun WhiteKey(
    note: Note,
    width: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(220.dp)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = whiteKeyLabel(note),
            modifier = Modifier.offset(y = (-12).dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlackKey(
    width: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(132.dp)
            .background(Color(0xFF111111), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .clickable(onClick = onClick),
    )
}

private fun blackKeyOffset(
    midi: Int,
    whiteKeys: List<PianoKey>,
    whiteKeyWidth: Dp,
    blackKeyWidth: Dp,
): Dp {
    val previousWhiteIndex = whiteKeys.indexOfLast { it.midi < midi }
    val centerOffset = (whiteKeyWidth - blackKeyWidth) / 2
    val base = whiteKeyWidth * (previousWhiteIndex + 1)
    return base - centerOffset
}

private fun whiteKeyLabel(note: Note): String {
    return if (note.name == "C") {
        "${note.name}${note.octave}"
    } else {
        ""
    }
}
