package com.example.skales.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
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

private const val InitialVisibleMidi = 48
private val PianoWhiteKeyColor = Color(0xFFF5F1E8)
private val PianoWhiteKeyLabelColor = Color(0xFF5A5348)
private val PianoKeyboardSurfaceColor = Color(0xFF0F0F12)

@Composable
fun PianoKeyboard(
    onNotePressed: (Int) -> Unit,
    modifier: Modifier = Modifier,
    whiteKeyHeight: Dp = 220.dp,
    blackKeyHeight: Dp = 132.dp,
    scrollState: ScrollState = rememberScrollState(),
) {
    val allKeys = remember { PianoLayout.keys() }
    val whiteKeys = remember(allKeys) { allKeys.filterNot { it.note.isBlackKey } }
    val blackKeys = remember(allKeys) { allKeys.filter { it.note.isBlackKey } }
    val density = LocalDensity.current
    val initialWhiteKeyIndex = remember(whiteKeys) {
        whiteKeys.indexOfFirst { it.midi == InitialVisibleMidi }.coerceAtLeast(0)
    }

    LaunchedEffect(initialWhiteKeyIndex) {
        val initialOffsetPx = with(density) { (PianoLayout.WhiteKeyWidth * initialWhiteKeyIndex).roundToPx() }
        scrollState.scrollTo(initialOffsetPx)
    }

    Box(
        modifier = modifier
            .background(PianoKeyboardSurfaceColor)
            .horizontalScroll(scrollState),
    ) {
        Box(
            modifier = Modifier
                .width(PianoLayout.totalWidth(allKeys))
                .background(PianoKeyboardSurfaceColor),
        ) {
            Row {
                whiteKeys.forEach { key ->
                    WhiteKey(
                        note = key.note,
                        width = PianoLayout.WhiteKeyWidth,
                        height = whiteKeyHeight,
                        onClick = { onNotePressed(key.midi) },
                    )
                }
            }

            blackKeys.forEach { key ->
                BlackKey(
                    width = key.width,
                    height = blackKeyHeight,
                    modifier = Modifier.offset(x = key.left),
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
    height: Dp,
    onClick: () -> Unit,
) {
    val keyShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = keyShape,
            )
            .background(PianoWhiteKeyColor, keyShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = whiteKeyLabel(note),
            modifier = Modifier.offset(y = (-12).dp),
            style = MaterialTheme.typography.labelSmall,
            color = PianoWhiteKeyLabelColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlackKey(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(Color(0xFF0C0C0D), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .clickable(onClick = onClick),
    )
}

private fun whiteKeyLabel(note: Note): String {
    return if (note.name == "C") "${note.name}${note.octave}" else ""
}
