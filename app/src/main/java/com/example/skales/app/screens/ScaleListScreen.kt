package com.example.skales.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.app.components.SkalesBackground
import com.example.skales.app.components.SkalesPanel
import com.example.skales.app.components.SkalesPill
import com.example.skales.app.components.SkalesPrimaryButton
import com.example.skales.app.components.SkalesSecondaryButton
import com.example.skales.app.components.SkalesSectionHeader
import com.example.skales.app.components.SkalesWordmark
import com.example.skales.app.viewmodel.ScaleListViewModel
import com.example.skales.model.Note
import com.example.skales.model.Scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleListScreen(
    viewModel: ScaleListViewModel,
    onCreateScale: () -> Unit,
    onOpenScale: (String) -> Unit,
    onEditScale: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var scalePendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Library", style = MaterialTheme.typography.titleLarge) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateScale,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text("New scale")
            }
        },
    ) { innerPadding ->
        SkalesBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.scales.isEmpty()) {
                EmptyState(
                    onCreateScale = onCreateScale,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        SkalesPanel {
                            SkalesWordmark(compact = true)
                            SkalesSectionHeader(
                                title = "A focused scale library",
                                supporting = "Build small practice sets, revisit them quickly, and keep the next action obvious.",
                            )
                        }
                    }
                    items(items = uiState.scales, key = { it.id }) { scale ->
                        ScaleListItem(
                            scale = scale,
                            onClick = { onOpenScale(scale.id) },
                            onEdit = { onEditScale(scale.id) },
                            onDelete = { scalePendingDelete = scale.id },
                        )
                    }
                }
            }
        }
    }

    if (scalePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { scalePendingDelete = null },
            title = { Text("Delete scale?") },
            text = { Text("This removes the saved scale from the app.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScale(scalePendingDelete.orEmpty())
                        scalePendingDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { scalePendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun EmptyState(
    onCreateScale: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        SkalesPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SkalesWordmark(compact = true)
                Text(
                    text = "No saved scales yet",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Start with one small scale, then shape the player and editor around your own practice flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SkalesPrimaryButton(
                    text = "Create your first scale",
                    onClick = onCreateScale,
                )
            }
        }
    }
}

@Composable
private fun ScaleListItem(
    scale: Scale,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val soundCount = scale.sets.sumOf { it.sounds.size }
    val previewNotes = scale.sets
        .flatMap { set -> set.sounds }
        .map { sound -> sound.midi }
        .take(8)
        .joinToString(separator = "  ") { midi ->
            val note = Note.fromMidi(midi)
            "${note.name}${note.octave}"
        }

    SkalesPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = scale.name, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkalesPill(text = "${scale.sets.size} sets")
                SkalesPill(text = "$soundCount sounds", highlighted = soundCount > 0)
            }
        }
        Text(
            text = if (previewNotes.isBlank()) "No sounds" else previewNotes,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkalesSecondaryButton(text = "Edit", onClick = onEdit)
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
