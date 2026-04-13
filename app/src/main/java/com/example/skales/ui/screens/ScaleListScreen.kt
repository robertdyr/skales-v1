package com.example.skales.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skales.domain.model.Note
import com.example.skales.domain.model.Scale
import com.example.skales.viewmodel.ScaleListViewModel

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
        topBar = {
            TopAppBar(title = { Text("Skales") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateScale) {
                Text("New")
            }
        },
    ) { innerPadding ->
        if (uiState.scales.isEmpty()) {
            EmptyState(
                onCreateScale = onCreateScale,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                    Text("Delete")
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "No saved scales yet",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Create a scale by tapping notes on the keyboard, then save it here.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onCreateScale) {
                Text("Create your first scale")
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
    val previewNotes = scale.sets
        .flatMap { set -> set.sounds }
        .flatMap { sound -> sound.notes }
        .take(8)
        .joinToString(separator = "  ") { midi ->
        val note = Note.fromMidi(midi)
        "${note.name}${note.octave}"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = scale.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (previewNotes.isBlank()) "No sounds" else previewNotes,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onEdit, modifier = Modifier.align(Alignment.End)) {
                Text("Edit")
            }
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Text("Delete")
            }
        }
    }
}
