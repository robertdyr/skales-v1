package com.example.skales.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.app.components.SkalesBackground
import com.example.skales.app.components.SkalesCircleButton
import com.example.skales.app.components.SkalesMetric
import com.example.skales.app.components.SkalesPanel
import com.example.skales.app.components.SkalesPill
import com.example.skales.app.components.SkalesPrimaryButton
import com.example.skales.app.components.SkalesSecondaryButton
import com.example.skales.app.components.SkalesSectionHeader
import com.example.skales.app.viewmodel.ScalePlayerViewModel
import com.example.skales.app.viewmodel.currentSetIndex
import com.example.skales.app.viewmodel.labelForSetSound
import com.example.skales.model.ScaleSet
import com.example.skales.player.PlaybackDirection

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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(scale?.name ?: "Play scale") },
                navigationIcon = {
                    SkalesSecondaryButton(text = "Back", onClick = onNavigateBack)
                },
                actions = {
                    if (scale != null) {
                        SkalesSecondaryButton(text = "Edit", onClick = { onEditScale(scale.id) })
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

        if (scale == null) {
            SkalesBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SkalesPanel { Text("Scale not found") }
                }
            }
            return@Scaffold
        }

        SkalesBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SkalesPanel {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SkalesPill(
                                text = if (uiState.direction == PlaybackDirection.Forward) "Ascending" else "Descending",
                                highlighted = true,
                            )
                            Text(
                                text = "Scale",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = scale.name,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    SkalesPanel {
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
                    }

                    SkalesPanel {
                        SkalesSectionHeader(
                            title = "Current set ${uiState.playbackCursor.currentSetIndex(scale, uiState.direction) + 1}",
                            supporting = "The highlighted circle tracks the active sound.",
                        )
                        SetSoundCircles(
                            set = uiState.currentSet,
                            currentSoundIndex = uiState.playbackCursor.soundIndexInSet,
                            isFinished = uiState.playbackCursor.isFinished,
                        )
                    }
                }

                SkalesPanel {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SkalesCircleButton(
                                label = "<",
                                onClick = { viewModel.setDirection(PlaybackDirection.Backward) },
                                highlighted = uiState.direction == PlaybackDirection.Backward,
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            SkalesCircleButton(
                                label = if (uiState.isPlaying) "||" else ">",
                                onClick = if (uiState.isPlaying) viewModel::stopPlayback else viewModel::play,
                                size = 108.dp,
                                highlighted = true,
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            SkalesCircleButton(
                                label = ">",
                                onClick = { viewModel.setDirection(PlaybackDirection.Forward) },
                                highlighted = uiState.direction == PlaybackDirection.Forward,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SkalesSecondaryButton(text = "Reset", onClick = viewModel::resetCursor)
                            SkalesPrimaryButton(
                                text = "Step once",
                                onClick = viewModel::step,
                                enabled = !uiState.isPlaying,
                            )
                            SkalesSecondaryButton(
                                text = "Stop",
                                onClick = viewModel::stopPlayback,
                                enabled = uiState.isPlaying,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetSoundCircles(
    set: ScaleSet?,
    currentSoundIndex: Int,
    isFinished: Boolean,
) {
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
            SkalesCircleButton(
                label = labelForSetSound(set, index),
                onClick = {},
                modifier = Modifier.padding(6.dp),
                size = 72.dp,
                highlighted = !isFinished && index == currentSoundIndex,
                enabled = false,
            )
        }
    }
}
