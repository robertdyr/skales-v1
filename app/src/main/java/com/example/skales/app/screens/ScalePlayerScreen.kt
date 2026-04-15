package com.example.skales.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.player.PlaybackDirection
import com.example.skales.model.ScaleSet
import com.example.skales.app.viewmodel.ScalePlayerViewModel
import com.example.skales.app.viewmodel.currentSetIndex
import com.example.skales.app.viewmodel.labelForSetSound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScalePlayerScreen(
    viewModel: ScalePlayerViewModel,
    onNavigateBack: () -> Unit,
    onEditScale: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scale = uiState.scale

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(scale?.name ?: "Play scale") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    if (scale != null) {
                        TextButton(onClick = { onEditScale(scale.id) }) {
                            Text("Edit")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!uiState.isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading...")
            }
            return@Scaffold
        }

        if (scale == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Scale not found")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SettingStepper(
                            label = "Tempo",
                            value = "${uiState.bpm} BPM",
                            onDecrease = viewModel::decreaseBpm,
                            onIncrease = viewModel::increaseBpm,
                        )
                        Text(
                            text = "Direction: ${if (uiState.direction == PlaybackDirection.Forward) "Up" else "Down"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Set ${uiState.playbackCursor.currentSetIndex(scale, uiState.direction) + 1}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SetSoundCircles(set = uiState.currentSet)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::play, enabled = !uiState.isPlaying) {
                        Text("Play")
                    }
                    TextButton(onClick = viewModel::stopPlayback, enabled = uiState.isPlaying) {
                        Text("Stop")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = { viewModel.setDirection(PlaybackDirection.Backward) }) {
                        Text("←")
                    }
                    Button(onClick = viewModel::step, enabled = !uiState.isPlaying) {
                        Text("Step")
                    }
                    Button(onClick = { viewModel.setDirection(PlaybackDirection.Forward) }) {
                        Text("→")
                    }
                }
            }
        }
    }
}

@Composable
private fun SetSoundCircles(set: ScaleSet?) {
    if (set == null || set.sounds.isEmpty()) {
        Text("No sounds", style = MaterialTheme.typography.bodyMedium)
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        set.sounds.forEachIndexed { index, _ ->
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelForSetSound(set, index),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
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
